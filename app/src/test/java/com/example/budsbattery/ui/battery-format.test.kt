package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.BatteryState
import org.junit.Assert.assertEquals
import org.junit.Test

private const val NOW_MS = 10_000_000_000L
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS

private val fullReading = BatteryReading(
    left = BatteryLevel(80, charging = false),
    right = BatteryLevel(75, charging = true),
    case = BatteryLevel(60, charging = false),
)

private fun live(reading: BatteryReading) = BatteryState(
    snapshot = BatterySnapshot(reading, savedAtMillis = NOW_MS),
    connected = true,
    connectedAtMillis = NOW_MS - MINUTE_MS,
)

class BatteryFormatTest {

    @Test
    fun `live tile shows buds and a live case`() {
        assertEquals("L 80 · R 75 ⚡ · Case 60", formatTileSubtitle(live(fullReading), NOW_MS))
    }

    @Test
    fun `live tile leaves out the case when the reading has none`() {
        val state = live(fullReading.copy(case = null))

        assertEquals("L 80 · R 75 ⚡", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `one bud in the ear shows one bud`() {
        assertEquals("R 75 ⚡", formatBudsLine(budsViewOf(live(fullReading.copy(left = null)), NOW_MS)))
    }

    @Test
    fun `disconnected tile shows the last values with their age`() {
        val state = BatteryState(BatterySnapshot(fullReading, savedAtMillis = NOW_MS - 2 * HOUR_MS), connected = false)

        assertEquals("L 80 · R 75 ⚡ · 2h ago", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `tile says reading while a new connection has no values yet`() {
        val state = BatteryState(
            snapshot = BatterySnapshot(fullReading, savedAtMillis = NOW_MS - HOUR_MS),
            connected = true,
            connectedAtMillis = NOW_MS - MINUTE_MS,
        )

        assertEquals("Reading…", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `tile says not read yet when nothing was saved`() {
        assertEquals("Not read yet", formatTileSubtitle(BatteryState(snapshot = null, connected = false), NOW_MS))
    }

    @Test
    fun `chip shows the bud that runs out first`() {
        assertEquals("75%", formatChipText(budsViewOf(live(fullReading), NOW_MS)))
        assertEquals("80%", formatChipText(budsViewOf(live(fullReading.copy(right = null)), NOW_MS)))
    }

    @Test
    fun `chip is empty while waiting or disconnected`() {
        val past = BatteryState(BatterySnapshot(fullReading, savedAtMillis = NOW_MS - HOUR_MS), connected = false)

        assertEquals("", formatChipText(budsViewOf(past, NOW_MS)))
        assertEquals("", formatChipText(budsViewOf(BatteryState(snapshot = null, connected = true), NOW_MS)))
    }

    @Test
    fun `age uses the right unit for each range`() {
        assertEquals("just now", formatAge(NOW_MS - 30_000L, NOW_MS))
        assertEquals("12 min ago", formatAge(NOW_MS - 12 * MINUTE_MS, NOW_MS))
        assertEquals("3h ago", formatAge(NOW_MS - 3 * HOUR_MS, NOW_MS))
        assertEquals("2d ago", formatAge(NOW_MS - 48 * HOUR_MS, NOW_MS))
    }
}
