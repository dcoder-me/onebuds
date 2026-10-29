package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.NoiseMode
import com.example.budsbattery.data.ReadSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTest {

    private val now = 1_000_000L

    private fun state(connected: Boolean = true, mode: NoiseMode? = NoiseMode.OFF, case: BatteryLevel? = null, budInCase: Boolean? = false) =
        BatteryState(
            snapshot = BatterySnapshot(BatteryReading(BatteryLevel(80, false), BatteryLevel(75, false), case), now, ReadSource.VENDOR_CHANNEL),
            connected = connected,
            connectedAtMillis = now - 1,
            budInCase = budInCase,
            noiseMode = mode,
        )

    @Test
    fun `widget size in dp maps to home screen cells`() {
        assertEquals(WidgetGrid(1, 1), widgetGridOf(60f, 60f))
        assertEquals(WidgetGrid(2, 1), widgetGridOf(120f, 60f))
        assertEquals(WidgetGrid(3, 1), widgetGridOf(190f, 60f))
        assertEquals(WidgetGrid(4, 1), widgetGridOf(260f, 60f))
        assertEquals(WidgetGrid(2, 2), widgetGridOf(120f, 120f))
        assertEquals(WidgetGrid(4, 2), widgetGridOf(260f, 120f))
    }

    @Test
    fun `buds widget follows the approved size table`() {
        assertEquals(BudsLayout.RING, budsLayoutOf(WidgetGrid(1, 1)))
        assertEquals(BudsLayout.RING_BUTTON, budsLayoutOf(WidgetGrid(2, 1)))
        assertEquals(BudsLayout.PAIR_BUTTON, budsLayoutOf(WidgetGrid(3, 1)))
        assertEquals(BudsLayout.TRIO_CHIP, budsLayoutOf(WidgetGrid(4, 1)))
        assertEquals(BudsLayout.SQUARE, budsLayoutOf(WidgetGrid(2, 2)))
        assertEquals(BudsLayout.WIDE, budsLayoutOf(WidgetGrid(4, 2)))
    }

    @Test
    fun `battery widget picks ring, tiles, bars or tall tiles by size`() {
        assertEquals(BatteryLayout.RING, batteryLayoutOf(WidgetGrid(1, 1)))
        assertEquals(BatteryLayout.TILES, batteryLayoutOf(WidgetGrid(2, 1)))
        assertEquals(BatteryLayout.BARS, batteryLayoutOf(WidgetGrid(4, 1)))
        assertEquals(BatteryLayout.TALL_TILES, batteryLayoutOf(WidgetGrid(2, 2)))
    }

    @Test
    fun `noise widget picks cycle, mode or switches by width`() {
        assertEquals(NoiseLayout.CYCLE, noiseLayoutOf(WidgetGrid(1, 1)))
        assertEquals(NoiseLayout.MODE, noiseLayoutOf(WidgetGrid(2, 1)))
        assertEquals(NoiseLayout.SWITCHES, noiseLayoutOf(WidgetGrid(3, 1)))
    }

    @Test
    fun `noise controls work only while connected with both buds out`() {
        val live = noiseControlOf(state(), now)
        assertTrue(live.enabled)
        assertEquals(NoiseMode.OFF, live.mode)

        val away = noiseControlOf(state(connected = false), now)
        assertFalse(away.enabled)
        assertEquals("Not connected", away.note)

        val inCase = noiseControlOf(state(case = BatteryLevel(60, false), budInCase = true), now)
        assertFalse(inCase.enabled)
        assertEquals("Buds in case", inCase.note)

        val reading = noiseControlOf(state(mode = null), now)
        assertFalse(reading.enabled)
        assertEquals("Reading…", reading.note)
    }
}
