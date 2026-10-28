package com.example.budsbattery.protocol

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun hex(text: String): ByteArray = text.split(" ").map { it.toInt(16).toByte() }.toByteArray()

private val batteryRequest = hex("AA 07 00 00 06 01 00 00 00")

class BudsProtocolTest {

    @Test
    fun `battery request has the documented bytes`() {
        assertArrayEquals(batteryRequest, encodeBatteryRequest(seq = 0))
    }

    @Test
    fun `battery subscription has the documented bytes`() {
        assertArrayEquals(hex("AA 09 00 00 05 02 00 02 00 09 01"), encodeBatterySubscription(seq = 0))
    }

    @Test
    fun `parses left, right and case with charging flag`() {
        val frame = hex("AA 0F 00 00 06 81 00 08 00 00 03 01 50 02 CB 03 3C")

        assertEquals(
            BatteryFrameResult.Reading(
                BatteryReading(
                    left = BatteryLevel(80, charging = false),
                    right = BatteryLevel(75, charging = true),
                    case = BatteryLevel(60, charging = false),
                ),
            ),
            parseBatteryFrame(frame),
        )
    }

    @Test
    fun `accepts the realme header marker 0x0004`() {
        val frame = hex("AA 0F 04 00 06 81 00 08 00 00 03 01 50 02 CB 03 3C")

        assertTrue(parseBatteryFrame(frame) is BatteryFrameResult.Reading)
    }

    @Test
    fun `empty case reads as unknown, not 0 percent`() {
        val frame = hex("AA 0F 00 00 06 81 00 08 00 00 03 01 50 02 CB 03 00")

        val result = parseBatteryFrame(frame) as BatteryFrameResult.Reading

        assertNull(result.reading.case)
    }

    @Test
    fun `level above 100 reads as unknown`() {
        val frame = hex("AA 0B 00 00 06 81 00 04 00 00 02 01 7F")

        val result = parseBatteryFrame(frame) as BatteryFrameResult.Reading

        assertNull(result.reading.left)
    }

    @Test
    fun `absent slot 0xFF is skipped and the other bud is kept`() {
        val frame = hex("AA 0D 00 00 06 81 00 06 00 00 02 FF 00 02 4B")

        val result = parseBatteryFrame(frame) as BatteryFrameResult.Reading

        assertNull(result.reading.left)
        assertEquals(BatteryLevel(75, charging = false), result.reading.right)
    }

    @Test
    fun `unknown battery index is rejected`() {
        val frame = hex("AA 0B 00 00 06 81 00 04 00 00 01 09 50")

        assertTrue(parseBatteryFrame(frame) is BatteryFrameResult.Rejected)
    }

    @Test
    fun `unexpected header marker is rejected`() {
        val frame = hex("AA 07 08 00 06 81 00 00 00")

        assertTrue(parseBatteryFrame(frame) is BatteryFrameResult.Rejected)
    }

    @Test
    fun `truncated frame is rejected`() {
        val frame = hex("AA 0F 00 00 06 81 00 08 00 00 03 01 50")

        assertTrue(parseBatteryFrame(frame) is BatteryFrameResult.Rejected)
    }

    @Test
    fun `other commands are ignored`() {
        val subscriptionAck = hex("AA 07 00 00 05 82 00 00 00")

        assertEquals(BatteryFrameResult.Ignored, parseBatteryFrame(subscriptionAck))
    }

    @Test
    fun `splits two complete frames and keeps the partial third`() {
        val split = splitFrames(batteryRequest + batteryRequest + hex("AA 0F 00"))

        assertEquals(2, split.frames.size)
        assertArrayEquals(hex("AA 0F 00"), split.leftover)
    }

    @Test
    fun `drops junk bytes before the preamble`() {
        val split = splitFrames(hex("00 11") + batteryRequest)

        assertEquals(1, split.frames.size)
        assertEquals(0, split.leftover.size)
    }

    @Test
    fun `leb128 round-trips a two-byte number`() {
        assertArrayEquals(hex("AC 02"), encodeLeb128(300))
        assertEquals(Leb128(value = 300, size = 2), decodeLeb128(hex("AC 02"), start = 0))
    }

    @Test
    fun `leb128 returns null when the number is cut short`() {
        assertNull(decodeLeb128(hex("AC"), start = 0))
    }
}
