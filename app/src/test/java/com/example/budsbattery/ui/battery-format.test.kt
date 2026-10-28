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

private fun live(reading: BatteryReading, caseSeenAt: Long = NOW_MS) = BatteryState(
    snapshot = BatterySnapshot(reading, savedAtMillis = NOW_MS, caseSeenAtMillis = caseSeenAt),
    connected = true,
    connectedAtMillis = NOW_MS - MINUTE_MS,
)

class BatteryFormatTest {

    @Test
    fun `live tile shows buds and a live case`() {
        assertEquals("L 80 · R 75 ⚡ · Case 60", formatTileSubtitle(live(fullReading), NOW_MS))
    }

    @Test
    fun `live tile leaves out a case that is only last seen`() {
        val state = live(fullReading, caseSeenAt = NOW_MS - HOUR_MS)

        assertEquals("L 80 · R 75 ⚡", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `one bud in the ear shows one bud`() {
        assertEquals("R 75 ⚡", formatBudsLine(budsViewOf(live(fullReading.copy(left = null), NOW_MS - HOUR_MS), NOW_MS)))
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
    fun `case line adds the age when the case is old`() {
        val view = budsViewOf(live(fullReading, caseSeenAt = NOW_MS - 2 * HOUR_MS), NOW_MS)

        assertEquals("Case 60 · seen 2h ago", formatCaseLine(view))
    }

    @Test
    fun `chip shows only live buds, short`() {
        assertEquals("L80 R75", formatChipText(budsViewOf(live(fullReading), NOW_MS)))
        assertEquals("R75", formatChipText(budsViewOf(live(fullReading.copy(left = null)), NOW_MS)))
    }

    @Test
    fun `age uses the right unit for each range`() {
        assertEquals("just now", formatAge(NOW_MS - 30_000L, NOW_MS))
        assertEquals("12 min ago", formatAge(NOW_MS - 12 * MINUTE_MS, NOW_MS))
        assertEquals("3h ago", formatAge(NOW_MS - 3 * HOUR_MS, NOW_MS))
        assertEquals("2d ago", formatAge(NOW_MS - 48 * HOUR_MS, NOW_MS))
    }
}
