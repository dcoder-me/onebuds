package com.example.budsbattery.protocol

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

class MessageStreamTest {

    @Test
    fun `reads the battery example from the Fast Pair spec`() {
        // Spec example 0x0303000357417F: left 87, right 65, case unknown
        val message = bytes(0x03, 0x03, 0x00, 0x03, 0x57, 0x41, 0x7F)

        val expected = BatteryReading(BatteryLevel(87, false), BatteryLevel(65, false), case = null)
        assertEquals(expected, parseMessageStreamBattery(message))
    }

    @Test
    fun `reads the charging bit`() {
        val message = bytes(0x03, 0x03, 0x00, 0x03, 0x80 or 40, 41, 0x80 or 70)

        val reading = parseMessageStreamBattery(message)

        assertEquals(BatteryLevel(40, true), reading?.left)
        assertEquals(BatteryLevel(70, true), reading?.case)
    }

    @Test
    fun `other messages are not battery`() {
        // Model ID event (group 3, code 1)
        assertNull(parseMessageStreamBattery(bytes(0x03, 0x01, 0x00, 0x03, 0x12, 0x34, 0x56)))
    }

    @Test
    fun `splits back-to-back messages and keeps a partial one for later`() {
        val first = bytes(0x03, 0x01, 0x00, 0x03, 0x12, 0x34, 0x56)
        val second = bytes(0x03, 0x03, 0x00, 0x03, 0x57, 0x41, 0x7F)
        val partial = bytes(0x03, 0x03, 0x00)

        val split = splitMessages(first + second + partial)

        assertEquals(2, split.messages.size)
        assertArrayEquals(second, split.messages[1])
        assertArrayEquals(partial, split.leftover)
    }

    @Test
    fun `length is big endian`() {
        val long = bytes(0x01, 0x02, 0x01, 0x00) + ByteArray(256)

        assertEquals(1, splitMessages(long).messages.size)
    }
}
