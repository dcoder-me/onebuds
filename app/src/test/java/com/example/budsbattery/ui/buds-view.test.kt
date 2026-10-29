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
private const val MODEL = "Realme Buds T310"

// A charging bud means it sits in the case, so the case is shown
private val reading = BatteryReading(BatteryLevel(80, true), BatteryLevel(75, false), BatteryLevel(60, false))
private val budsOut = BatteryReading(BatteryLevel(80, false), BatteryLevel(75, false), BatteryLevel(60, false))

private fun stateOf(
    savedAt: Long,
    connected: Boolean,
    value: BatteryReading = reading,
    connectedAt: Long = NOW_MS - MINUTE_MS,
    budInCase: Boolean? = null,
) = BatteryState(BatterySnapshot(value, savedAt), connected, connectedAt, budInCase)

class BudsViewTest {

    @Test
    fun `values from the last connection are never shown, the case included`() {
        val view = budsViewOf(stateOf(savedAt = NOW_MS - HOUR_MS, connected = true), NOW_MS)

        assertTrue(view.waiting)
        assertNull(view.left)
        assertNull(view.right)
        assertNull(view.case)
    }

    @Test
    fun `tile, chip and status all say live for a fresh reading`() {
        val state = stateOf(savedAt = NOW_MS, connected = true)
        val view = budsViewOf(state, NOW_MS)

        assertTrue(view.left!!.live && view.right!!.live && view.case!!.live)
        assertEquals("Connected", view.status)
        assertEquals("75%", formatChipText(view))
        assertEquals("L 80 ⚡ · R 75 · Case 60", formatTileSubtitle(state, NOW_MS))
    }

    @Test
    fun `every surface drops the case together once the buds leave it`() {
        val state = stateOf(savedAt = NOW_MS, connected = true, value = budsOut.copy(case = null))
        val view = budsViewOf(state, NOW_MS)

        assertNull(view.case)
        assertEquals(BudsShape.TWO_BUDS, view.shape)
        assertEquals("L 80 · R 75", formatTileSubtitle(state, NOW_MS))
        assertEquals("Connected", liveUpdateTextOf(view, MODEL).text)
    }

    @Test
    fun `a stale case value is hidden while both buds are out of the case`() {
        val state = stateOf(savedAt = NOW_MS, connected = true, value = budsOut)
        val view = budsViewOf(state, NOW_MS)

        assertNull(view.case)
        assertEquals("L 80 · R 75", formatTileSubtitle(state, NOW_MS))
        assertEquals("Connected", liveUpdateTextOf(view, MODEL).text)
    }

    @Test
    fun `full buds in the case show the case, though they no longer charge`() {
        val view = budsViewOf(stateOf(NOW_MS, connected = true, value = budsOut, budInCase = true), NOW_MS)

        assertEquals(60, view.case?.level?.percent)
    }

    @Test
    fun `the buds' own in-case status wins over the charging guess`() {
        assertNull(budsViewOf(stateOf(NOW_MS, connected = true, budInCase = false), NOW_MS).case)
    }

    @Test
    fun `the case is never shown once the buds disconnect`() {
        assertNull(budsViewOf(stateOf(NOW_MS, connected = false), NOW_MS).case)
    }

    @Test
    fun `shape follows what is reachable`() {
        val withCase = budsViewOf(stateOf(NOW_MS, connected = true), NOW_MS)
        val oneBud = budsViewOf(stateOf(NOW_MS, connected = true, value = reading.copy(right = null, case = null)), NOW_MS)

        assertEquals(BudsShape.WITH_CASE, withCase.shape)
        assertEquals(BudsShape.ONE_BUD, oneBud.shape)
    }

    @Test
    fun `disconnected shows last values, none live`() {
        val view = budsViewOf(stateOf(NOW_MS - 2 * HOUR_MS, connected = false), NOW_MS)

        assertFalse(view.left!!.live)
        assertEquals("Not connected · read 2h ago", view.status)
    }

    @Test
    fun `just connected says connecting`() {
        assertEquals("Connecting…", budsViewOf(BatteryState(snapshot = null, connected = true), NOW_MS).status)
    }
}
