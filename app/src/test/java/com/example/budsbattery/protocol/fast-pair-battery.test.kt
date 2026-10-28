package com.example.budsbattery.protocol

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Bytes follow the Fast Pair spec tables: flags, filter field (0bLLLL0000), salt (0x21 + 2 bytes), battery
private fun advert(vararg bytes: Int): ByteArray = ByteArray(bytes.size) { bytes[it].toByte() }

class FastPairBatteryTest {

    @Test
    fun `reads left right and case with the charging bit`() {
        val data = advert(0x00, 0x40, 1, 2, 3, 4, 0x21, 0xAB, 0xCD, 0x33, 80, 0x80 or 75, 60)

        val expected = BatteryReading(
            left = BatteryLevel(80, charging = false),
            right = BatteryLevel(75, charging = true),
            case = BatteryLevel(60, charging = false),
        )
        assertEquals(expected, parseFastPairBattery(data))
    }

    @Test
    fun `hide ui battery type is read too`() {
        val data = advert(0x00, 0x22, 9, 9, 0x21, 1, 2, 0x34, 50, 51, 52)

        assertEquals(BatteryLevel(52, charging = false), parseFastPairBattery(data)?.case)
    }

    @Test
    fun `unknown value 0x7F becomes null`() {
        val data = advert(0x00, 0x10, 7, 0x21, 1, 2, 0x33, 40, 41, 0x7F)

        assertNull(parseFastPairBattery(data)?.case)
        assertEquals(BatteryLevel(40, charging = false), parseFastPairBattery(data)?.left)
    }

    @Test
    fun `model id advert has no battery`() {
        assertNull(parseFastPairBattery(advert(0x0A, 0x2B, 0x3C)))
    }

    @Test
    fun `advert without a battery field gives null`() {
        assertNull(parseFastPairBattery(advert(0x00, 0x20, 1, 2, 0x21, 3, 4)))
    }

    @Test
    fun `truncated field gives null instead of crashing`() {
        assertNull(parseFastPairBattery(advert(0x00, 0x40, 1, 2)))
    }
}
