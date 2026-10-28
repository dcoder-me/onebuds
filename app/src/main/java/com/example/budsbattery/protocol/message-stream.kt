package com.example.budsbattery.protocol

import com.example.budsbattery.data.BatteryReading

// === NOTE: === Layout from developers.google.com/nearby/fast-pair/specifications (Message Stream, Device information).
// Message: group (1) | code (1) | data length (2, big endian) | data
private const val HEADER_BYTES = 4
private const val GROUP_DEVICE_INFORMATION = 0x03
private const val CODE_BATTERY_UPDATED = 0x03
private const val BYTE_MASK = 0xFF
private const val BYTE_BITS = 8

data class MessageSplit(val messages: List<ByteArray>, val leftover: ByteArray)

/**
 * Cuts complete Message Stream messages out of a byte buffer. A partial message stays in `leftover`.
 *
 * @param buffer - bytes read so far from the socket
 * @returns the complete messages and the bytes that still need more data
 */
fun splitMessages(buffer: ByteArray): MessageSplit {
    val messages = mutableListOf<ByteArray>()
    var offset = 0
    while (buffer.size - offset >= HEADER_BYTES) {
        val dataLength = ((buffer[offset + 2].toInt() and BYTE_MASK) shl BYTE_BITS) or (buffer[offset + 3].toInt() and BYTE_MASK)
        val end = offset + HEADER_BYTES + dataLength
        if (end > buffer.size) break
        messages += buffer.copyOfRange(offset, end)
        offset = end
    }
    return MessageSplit(messages, buffer.copyOfRange(offset, buffer.size))
}

/**
 * Reads the "battery updated" event the buds send when the stream opens and on every change.
 *
 * @param message - exactly one message, as returned by splitMessages
 * @returns the reading, or null for any other message
 */
fun parseMessageStreamBattery(message: ByteArray): BatteryReading? {
    if (message.size <= HEADER_BYTES) return null
    val group = message[0].toInt() and BYTE_MASK
    val code = message[1].toInt() and BYTE_MASK
    if (group != GROUP_DEVICE_INFORMATION || code != CODE_BATTERY_UPDATED) return null
    return decodeBatteryValues(message.copyOfRange(HEADER_BYTES, message.size))
}
