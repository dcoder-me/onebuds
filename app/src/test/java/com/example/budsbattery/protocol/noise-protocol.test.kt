package com.example.budsbattery.protocol

import com.example.budsbattery.data.NoiseMode

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

private fun hex(text: String): ByteArray = text.split(" ").map { it.toInt(16).toByte() }.toByteArray()

// Wire values from Realme Link (TLHeadsetConnectionImp.O5): 08 noise cancelling, 01 off, 02 transparency
class NoiseProtocolTest {

    @Test
    fun `noise query asks for the main noise setting`() {
        assertArrayEquals(hex("AA 09 00 00 0C 01 00 02 00 01 01"), encodeNoiseQuery(seq = 0))
    }

    @Test
    fun `setting noise cancelling sends value 08`() {
        assertArrayEquals(hex("AA 0A 00 00 04 04 00 03 00 01 01 08"), encodeNoiseSet(seq = 0, NoiseMode.NOISE_CANCELLING))
    }

    @Test
    fun `setting transparency sends value 02 and off sends 01`() {
        assertArrayEquals(hex("AA 0A 00 00 04 04 00 03 00 01 01 02"), encodeNoiseSet(seq = 0, NoiseMode.TRANSPARENCY))
        assertArrayEquals(hex("AA 0A 00 00 04 04 00 03 00 01 01 01"), encodeNoiseSet(seq = 0, NoiseMode.OFF))
    }

    @Test
    fun `noise reply gives the current mode`() {
        // Payload: status 00 | action 01 | type 01 | value 08
        val frame = hex("AA 0B 04 00 0C 81 00 04 00 00 01 01 08")

        assertEquals(BatteryFrameResult.Noise(NoiseMode.NOISE_CANCELLING), parseBatteryFrame(frame))
    }

    @Test
    fun `noise push from a long press gives the new mode`() {
        // Payload: event 03 | action 01 | type 01 | value 02
        val frame = hex("AA 0B 08 00 04 02 07 04 00 03 01 01 02")

        assertEquals(BatteryFrameResult.Noise(NoiseMode.TRANSPARENCY), parseBatteryFrame(frame))
    }

    @Test
    fun `short noise push without the action byte still reads`() {
        // Payload: event 03 | type 01 | value 01
        val frame = hex("AA 0A 08 00 04 02 07 03 00 03 01 01")

        assertEquals(BatteryFrameResult.Noise(NoiseMode.OFF), parseBatteryFrame(frame))
    }

    @Test
    fun `long-press cycle settings are not the current mode`() {
        // Action 02 describes which modes a long press cycles through
        val frame = hex("AA 0B 08 00 04 02 07 04 00 03 02 01 0B")

        assertEquals(BatteryFrameResult.Ignored, parseBatteryFrame(frame))
    }

    @Test
    fun `failed noise reply is rejected`() {
        val frame = hex("AA 0B 04 00 0C 81 00 04 00 01 01 01 08")

        assertEquals(BatteryFrameResult.Rejected("Buds reported a non-zero status"), parseBatteryFrame(frame))
    }

    @Test
    fun `subscription also asks for noise changes`() {
        // 03 = how many events follow: battery, earbuds status, noise
        assertArrayEquals(hex("AA 0B 00 00 05 02 00 04 00 03 01 02 03"), encodeBatterySubscription(seq = 0))
    }
}
