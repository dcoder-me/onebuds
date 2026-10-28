package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.BatteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NOW_MS = 10_000_000_000L
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS

private val reading = BatteryReading(BatteryLevel(80, false), BatteryLevel(75, false), BatteryLevel(60, false))

private fun stateOf(savedAt: Long, caseSeenAt: Long, connected: Boolean, connectedAt: Long = NOW_MS - MINUTE_MS) =
    BatteryState(BatterySnapshot(reading, savedAt, caseSeenAt), connected, connectedAt)

class BudsViewTest {

    @Test
    fun `values from the last connection are never shown as live buds`() {
        val view = budsViewOf(stateOf(savedAt = NOW_MS - HOUR_MS, caseSeenAt = NOW_MS - HOUR_MS, connected = true), NOW_MS)

        assertTrue(view.waiting)
        assertNull(view.left)
        assertNull(view.right)
        assertFalse(view.case!!.live)
        assertEquals("seen 1h ago", view.case!!.age)
    }

    @Test
    fun `tile, chip and status all say live for a fresh reading`() {
        val state = stateOf(savedAt = NOW_MS, caseSeenAt = NOW_MS, connected = true)
        val view = budsViewOf(state, NOW_MS)

        assertTrue(view.left!!.live && view.right!!.live && view.case!!.live)
        assertEquals("Connected · updated just now", view.status)
        assertEquals("L80 R75", formatChipText(view))
        assertEquals("L 80 · R 75 · Case 60", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `shape follows what is reachable`() {
        val withCase = budsViewOf(stateOf(NOW_MS, NOW_MS, connected = true), NOW_MS)
        val noCase = budsViewOf(stateOf(NOW_MS, NOW_MS - HOUR_MS, connected = true), NOW_MS)
        val oneBud = budsViewOf(
            BatteryState(BatterySnapshot(reading.copy(right = null), NOW_MS, NOW_MS - HOUR_MS), true, NOW_MS - MINUTE_MS),
            NOW_MS,
        )

        assertEquals(BudsShape.WITH_CASE, withCase.shape)
        assertEquals(BudsShape.TWO_BUDS, noCase.shape)
        assertEquals(BudsShape.ONE_BUD, oneBud.shape)
    }

    @Test
    fun `disconnected shows last values, none live`() {
        val view = budsViewOf(stateOf(NOW_MS - 2 * HOUR_MS, NOW_MS - 2 * HOUR_MS, connected = false), NOW_MS)

        assertFalse(view.left!!.live)
        assertEquals("Not connected · read 2h ago", view.status)
    }
}
