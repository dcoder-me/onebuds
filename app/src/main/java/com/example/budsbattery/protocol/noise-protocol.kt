package com.example.budsbattery.protocol

import com.example.budsbattery.data.NoiseMode

// === NOTE: === Layout from Realme Link (SetCommandManager.a, NotificationCommandManager event 3). Each noise
// payload is action | type | value; only action 01 is the mode in use, action 02 is the long-press cycle list
private const val COMMAND_NOISE_QUERY = 0x010C
internal const val COMMAND_NOISE_REPLY = 0x810C
private const val COMMAND_NOISE_SET = 0x0404

private const val ACTION_CURRENT_MODE = 0x01
private const val TYPE_MAIN = 0x01
private const val PAYLOAD_STATUS_OK = 0x00

// A push is event | action | type | value; older firmware leaves the action out (event | type | value)
private const val PUSH_FULL_BYTES = 4
private const val PUSH_SHORT_BYTES = 3
private const val REPLY_BYTES = 4

// Realme Link sends 08 for noise cancelling, 01 for off and 02 for transparency (TLHeadsetConnectionImp.O5)
private val WIRE_VALUES = mapOf(NoiseMode.NOISE_CANCELLING to 0x08, NoiseMode.OFF to 0x01, NoiseMode.TRANSPARENCY to 0x02)

/**
 * Builds the request that asks the buds which noise mode is on.
 *
 * @param seq - frame counter; the buds echo it back in the reply
 * @returns the raw bytes to write to the socket
 */
fun encodeNoiseQuery(seq: Int): ByteArray =
    encodeFrame(COMMAND_NOISE_QUERY, seq, byteArrayOf(ACTION_CURRENT_MODE.toByte(), TYPE_MAIN.toByte()))

/**
 * Builds the command that switches the buds to a noise mode.
 *
 * @param seq - frame counter; the buds echo it back in the reply
 * @param mode - mode to switch to
 * @returns the raw bytes to write to the socket
 */
fun encodeNoiseSet(seq: Int, mode: NoiseMode): ByteArray =
    encodeFrame(COMMAND_NOISE_SET, seq, byteArrayOf(ACTION_CURRENT_MODE.toByte(), TYPE_MAIN.toByte(), WIRE_VALUES.getValue(mode).toByte()))

internal fun parseNoiseReply(payload: ByteArray): BatteryFrameResult {
    if (payload.size < REPLY_BYTES) return rejected("Noise payload too short")
    if (payload[0].unsigned() != PAYLOAD_STATUS_OK) return rejected("Buds reported a non-zero status")
    return noiseResult(action = payload[1].unsigned(), value = payload[3].unsigned())
}

internal fun parseNoisePush(payload: ByteArray): BatteryFrameResult = when {
    payload.size >= PUSH_FULL_BYTES -> noiseResult(action = payload[1].unsigned(), value = payload[3].unsigned())
    payload.size == PUSH_SHORT_BYTES -> noiseResult(action = ACTION_CURRENT_MODE, value = payload[2].unsigned())
    else -> rejected("Noise push too short")
}

// Values the T310 doesn't offer (adaptive, strength levels) are ignored rather than shown as the wrong mode
private fun noiseResult(action: Int, value: Int): BatteryFrameResult {
    if (action != ACTION_CURRENT_MODE) return BatteryFrameResult.Ignored
    val mode = WIRE_VALUES.entries.firstOrNull { it.value == value }?.key ?: return BatteryFrameResult.Ignored
    return BatteryFrameResult.Noise(mode)
}
