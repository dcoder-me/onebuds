package com.example.budsbattery.protocol

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading

// === NOTE: === Oppo-family frame layout (little-endian), checked against Gadgetbridge's Oppo code.
// Frame: AA | LEB128 length | 00 00 or 04 00 | command (2) | seq (1) | payload length (2) | payload

const val PREAMBLE: Byte = 0xAA.toByte()

private const val BYTE_MASK = 0xFF
private const val SHORT_BYTE_BITS = 8
private const val HEADER_BYTES = 7
private val ACCEPTED_HEADER_MARKERS = setOf(0x0000, 0x0004)

private const val COMMAND_BATTERY_REQUEST = 0x0106
private const val COMMAND_BATTERY_REPLY = 0x8106
private const val COMMAND_SUBSCRIPTION_SET = 0x0205
private const val COMMAND_SUBSCRIPTION_PUSH = 0x0204
private const val SUBSCRIPTION_MARKER = 0x09
private const val SUBSCRIPTION_BATTERY = 0x01

private const val PAYLOAD_STATUS_OK = 0x00
private const val PAYLOAD_PREFIX_BYTES = 2
private const val PAIR_BYTES = 2

private const val INDEX_LEFT = 0x01
private const val INDEX_RIGHT = 0x02
private const val INDEX_CASE = 0x03
private const val INDEX_ABSENT = 0xFF

private const val LEVEL_MASK = 0x7F
private const val CHARGING_BIT = 0x80
private const val MAX_PERCENT = 100
private const val CASE_EMPTY_PLACEHOLDER = 0

private const val LEB128_VALUE_MASK = 0x7F
private const val LEB128_CONTINUE_BIT = 0x80
private const val LEB128_BITS_PER_BYTE = 7

/** Result of reading one frame from the buds. */
sealed interface BatteryFrameResult {
    data class Reading(val reading: BatteryReading) : BatteryFrameResult
    data class Rejected(val reason: String) : BatteryFrameResult
    data object Ignored : BatteryFrameResult
}

data class Leb128(val value: Int, val size: Int)

data class FrameSplit(val frames: List<ByteArray>, val leftover: ByteArray)

/**
 * Builds the request that asks the buds for their battery levels.
 *
 * @param seq - frame counter; the buds echo it back in the reply
 * @returns the raw bytes to write to the socket
 */
fun encodeBatteryRequest(seq: Int): ByteArray = encodeFrame(COMMAND_BATTERY_REQUEST, seq, ByteArray(0))

/**
 * Builds the request that makes the buds push battery changes without being asked.
 *
 * @param seq - frame counter; the buds echo it back in the reply
 * @returns the raw bytes to write to the socket
 */
fun encodeBatterySubscription(seq: Int): ByteArray = encodeFrame(
    COMMAND_SUBSCRIPTION_SET,
    seq,
    byteArrayOf(SUBSCRIPTION_MARKER.toByte(), SUBSCRIPTION_BATTERY.toByte()),
)

/**
 * Cuts complete frames out of a byte buffer. Partial frames stay in `leftover`
 * until more bytes arrive. Bytes before a preamble are dropped.
 *
 * @param buffer - bytes read so far from the socket
 * @returns the complete frames and the bytes that still need more data
 */
fun splitFrames(buffer: ByteArray): FrameSplit {
    val frames = mutableListOf<ByteArray>()
    var offset = 0
    while (offset < buffer.size) {
        if (buffer[offset] != PREAMBLE) {
            offset++
            continue
        }
        val length = decodeLeb128(buffer, offset + 1) ?: break
        val end = offset + 1 + length.size + length.value
        if (end > buffer.size) break
        frames += buffer.copyOfRange(offset, end)
        offset = end
    }
    return FrameSplit(frames, buffer.copyOfRange(offset, buffer.size))
}

/**
 * Reads one complete frame and turns the battery part into values.
 *
 * @param frame - exactly one frame, as returned by splitFrames
 * @returns a reading, a rejection with the reason, or Ignored for other commands
 */
fun parseBatteryFrame(frame: ByteArray): BatteryFrameResult {
    val length = decodeLeb128(frame, 1) ?: return rejected("Header cut short")
    val body = frame.copyOfRange(1 + length.size, frame.size)
    if (body.size != length.value) return rejected("Length does not match frame")
    if (body.size < HEADER_BYTES) return rejected("Header cut short")

    val marker = readShortLe(body, 0)
    if (marker !in ACCEPTED_HEADER_MARKERS) return rejected("Unexpected header 0x%04X".format(marker))
    val command = readShortLe(body, 2)
    if (command != COMMAND_BATTERY_REPLY && command != COMMAND_SUBSCRIPTION_PUSH) return BatteryFrameResult.Ignored
    if (readShortLe(body, 5) != body.size - HEADER_BYTES) return rejected("Payload length mismatch")

    val payload = body.copyOfRange(HEADER_BYTES, body.size)
    // A push starts with its subscription type instead of a status byte; only battery pushes matter here
    if (command == COMMAND_SUBSCRIPTION_PUSH) {
        if (payload.firstOrNull()?.unsigned() != SUBSCRIPTION_BATTERY) return BatteryFrameResult.Ignored
        return parseBatteryPairs(payload)
    }
    return parseBatteryPayload(payload)
}

/**
 * Decodes an unsigned LEB128 number.
 *
 * @param buffer - bytes to read from
 * @param start - index of the first byte of the number
 * @returns the value and how many bytes it used, or null if the bytes ran out
 */
fun decodeLeb128(buffer: ByteArray, start: Int): Leb128? {
    var value = 0
    var shift = 0
    for (index in start until buffer.size) {
        val byte = buffer[index].unsigned()
        value = value or ((byte and LEB128_VALUE_MASK) shl shift)
        if ((byte and LEB128_CONTINUE_BIT) == 0) return Leb128(value, index - start + 1)
        shift += LEB128_BITS_PER_BYTE
    }
    return null
}

/**
 * Encodes a number as unsigned LEB128.
 *
 * @param value - non-negative number to encode
 * @returns the encoded bytes
 */
fun encodeLeb128(value: Int): ByteArray {
    val bytes = mutableListOf<Byte>()
    var remaining = value
    do {
        val low = remaining and LEB128_VALUE_MASK
        remaining = remaining ushr LEB128_BITS_PER_BYTE
        val byte = if (remaining != 0) low or LEB128_CONTINUE_BIT else low
        bytes += byte.toByte()
    } while (remaining != 0)
    return bytes.toByteArray()
}

private fun encodeFrame(command: Int, seq: Int, payload: ByteArray): ByteArray {
    val header = byteArrayOf(0x00, 0x00) + shortLe(command) + byteArrayOf(seq.toByte()) + shortLe(payload.size)
    val body = header + payload
    return byteArrayOf(PREAMBLE) + encodeLeb128(body.size) + body
}

private fun parseBatteryPayload(payload: ByteArray): BatteryFrameResult {
    if (payload.size < PAYLOAD_PREFIX_BYTES) return rejected("Battery payload too short")
    if (payload[0].unsigned() != PAYLOAD_STATUS_OK) return rejected("Buds reported a non-zero status")
    return parseBatteryPairs(payload)
}

// Same layout after the first byte in a reply and a push: count, then index/level pairs (Gadgetbridge parseBattery)
private fun parseBatteryPairs(payload: ByteArray): BatteryFrameResult {
    if (payload.size < PAYLOAD_PREFIX_BYTES) return rejected("Battery payload too short")
    if ((payload.size - PAYLOAD_PREFIX_BYTES) % PAIR_BYTES != 0) return rejected("Battery pairs cut short")

    var reading = BatteryReading(left = null, right = null, case = null)
    for (pair in payload.drop(PAYLOAD_PREFIX_BYTES).chunked(PAIR_BYTES)) {
        val index = pair[0].unsigned()
        if (index == INDEX_ABSENT) continue
        if (index !in INDEX_LEFT..INDEX_CASE) return rejected("Unknown battery index $index")
        reading = withLevel(reading, index, decodeLevel(pair[1].unsigned(), isCase = index == INDEX_CASE))
    }
    return BatteryFrameResult.Reading(reading)
}

// Out-of-range levels and an empty case become null, so the UI never shows a fake 0%
private fun decodeLevel(raw: Int, isCase: Boolean): BatteryLevel? {
    val percent = raw and LEVEL_MASK
    val charging = (raw and CHARGING_BIT) != 0
    if (percent > MAX_PERCENT) return null
    if (isCase && percent == CASE_EMPTY_PLACEHOLDER) return null
    return BatteryLevel(percent, charging)
}

private fun withLevel(reading: BatteryReading, index: Int, level: BatteryLevel?): BatteryReading = when (index) {
    INDEX_LEFT -> reading.copy(left = level)
    INDEX_RIGHT -> reading.copy(right = level)
    else -> reading.copy(case = level)
}

private fun readShortLe(buffer: ByteArray, offset: Int): Int =
    buffer[offset].unsigned() or (buffer[offset + 1].unsigned() shl SHORT_BYTE_BITS)

private fun shortLe(value: Int): ByteArray = byteArrayOf(
    (value and BYTE_MASK).toByte(),
    ((value shr SHORT_BYTE_BITS) and BYTE_MASK).toByte(),
)

private fun Byte.unsigned(): Int = toInt() and BYTE_MASK

private fun rejected(reason: String): BatteryFrameResult = BatteryFrameResult.Rejected(reason)
