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

class BatteryFormatTest {

    @Test
    fun `line shows every known part with the charging mark`() {
        assertEquals("L 80 · R 75 ⚡ · Case 60", formatBatteryLine(fullReading))
    }

    @Test
    fun `line leaves out unknown parts`() {
        val reading = fullReading.copy(right = null)

        assertEquals("L 80 · Case 60", formatBatteryLine(reading))
    }

    @Test
    fun `connected tile shows the line without an age`() {
        val state = BatteryState(BatterySnapshot(fullReading, savedAtMillis = NOW_MS), connected = true)

        assertEquals("L 80 · R 75 ⚡ · Case 60", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `disconnected tile shows the last values with their age`() {
        val state = BatteryState(BatterySnapshot(fullReading, savedAtMillis = NOW_MS - 2 * HOUR_MS), connected = false)

        assertEquals("L 80 · R 75 ⚡ · Case 60 · 2h ago", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `tile says not read yet when nothing was saved`() {
        val state = BatteryState(snapshot = null, connected = false)

        assertEquals("Not read yet", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `tile says not read yet when every part is unknown`() {
        val empty = BatteryReading(left = null, right = null, case = null)
        val state = BatteryState(BatterySnapshot(empty, savedAtMillis = NOW_MS), connected = true)

        assertEquals("Not read yet", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `age uses the right unit for each range`() {
        assertEquals("just now", formatAge(NOW_MS - 30_000L, NOW_MS))
        assertEquals("12 min ago", formatAge(NOW_MS - 12 * MINUTE_MS, NOW_MS))
        assertEquals("3h ago", formatAge(NOW_MS - 3 * HOUR_MS, NOW_MS))
        assertEquals("2d ago", formatAge(NOW_MS - 48 * HOUR_MS, NOW_MS))
    }
}
