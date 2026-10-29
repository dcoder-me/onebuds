package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.NoiseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoiseFormatTest {

    @Test
    fun `card names the mode the buds report`() {
        val state = BatteryState(snapshot = null, connected = true, noiseMode = NoiseMode.TRANSPARENCY)

        assertEquals(NoiseMode.TRANSPARENCY, noiseModeOf(state))
        assertEquals("Transparency", formatNoiseSubtitle(state))
    }

    @Test
    fun `card waits for the buds before naming a mode`() {
        assertEquals("Reading…", formatNoiseSubtitle(BatteryState(snapshot = null, connected = true)))
    }

    @Test
    fun `a mode is never shown once the buds disconnect`() {
        val state = BatteryState(snapshot = null, connected = false, noiseMode = NoiseMode.NOISE_CANCELLING)

        assertNull(noiseModeOf(state))
        assertEquals("Not connected", formatNoiseSubtitle(state))
    }

    @Test
    fun `every mode has its own hint`() {
        val hints = NoiseMode.entries.map(::noiseHint)

        assert(hints.none(String::isBlank))
        assertEquals(hints.size, hints.toSet().size)
    }

    @Test
    fun `tapping the active mode steps through every mode and wraps around`() {
        assertEquals(NoiseMode.OFF, nextNoiseMode(NoiseMode.NOISE_CANCELLING))
        assertEquals(NoiseMode.TRANSPARENCY, nextNoiseMode(NoiseMode.OFF))
        assertEquals(NoiseMode.NOISE_CANCELLING, nextNoiseMode(NoiseMode.TRANSPARENCY))
    }

    @Test
    fun `small widgets use the short names from the design`() {
        assertEquals(listOf("ANC", "Off", "Aware"), NoiseMode.entries.map(::noiseShortLabel))
    }
}
