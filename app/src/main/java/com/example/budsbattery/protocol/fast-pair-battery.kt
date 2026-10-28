package com.example.budsbattery.protocol

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading

// Layout from developers.google.com/nearby/fast-pair/specifications (provider advertising + battery notification)
private const val VERSION_AND_FLAGS = 0x00
private const val MODEL_ID_ADVERT_SIZE = 3
private const val TYPE_BATTERY_SHOW_UI = 0b0011
private const val TYPE_BATTERY_HIDE_UI = 0b0100
private const val NIBBLE_MASK = 0x0F
private const val NIBBLE_SHIFT = 4
private const val BYTE_MASK = 0xFF
private const val CHARGING_BIT = 0x80
private const val LEVEL_MASK = 0x7F
private const val MAX_PERCENT = 100
private const val LEFT = 0
private const val RIGHT = 1
private const val CASE = 2

/**
 * Reads left, right and case battery from Fast Pair service data (UUID 0xFE2C).
 *
 * @description The not-discoverable advert is a flags byte, then fields headed by 0bLLLLTTTT
 * (L = length, T = type): account key filter, salt, then battery (type 0b0011 or 0b0100).
 * The 3-byte discoverable advert only holds a model ID and has no battery.
 * @param serviceData - service data bytes for UUID 0xFE2C
 * @returns the reading, or null when the advert has no battery field or is malformed
 */
fun parseFastPairBattery(serviceData: ByteArray): BatteryReading? {
    if (serviceData.size <= MODEL_ID_ADVERT_SIZE) return null
    if (serviceData[0].toInt() and BYTE_MASK != VERSION_AND_FLAGS) return null
    var index = 1
    while (index < serviceData.size) {
        val header = serviceData[index].toInt() and BYTE_MASK
        val length = header shr NIBBLE_SHIFT
        val type = header and NIBBLE_MASK
        val end = index + 1 + length
        if (end > serviceData.size) return null
        if (type == TYPE_BATTERY_SHOW_UI || type == TYPE_BATTERY_HIDE_UI) {
            return decodeBatteryValues(serviceData.copyOfRange(index + 1, end))
        }
        index = end
    }
    return null
}

/**
 * Decodes the Fast Pair battery bytes: left, right, case, each 0bSVVVVVVV.
 *
 * @param values - up to three battery bytes, as in the advert and the Message Stream event
 * @returns the reading, or null when every part is unknown
 */
fun decodeBatteryValues(values: ByteArray): BatteryReading? {
    val reading = BatteryReading(
        left = values.getOrNull(LEFT)?.let(::decodeFastPairLevel),
        right = values.getOrNull(RIGHT)?.let(::decodeFastPairLevel),
        case = values.getOrNull(CASE)?.let(::decodeFastPairLevel),
    )
    return reading.takeIf { it.left != null || it.right != null || it.case != null }
}

// 0bSVVVVVVV: S = charging, V = 0..100; 0b1111111 means unknown
private fun decodeFastPairLevel(value: Byte): BatteryLevel? {
    val raw = value.toInt() and BYTE_MASK
    val percent = raw and LEVEL_MASK
    if (percent > MAX_PERCENT) return null
    return BatteryLevel(percent = percent, charging = raw and CHARGING_BIT != 0)
}
