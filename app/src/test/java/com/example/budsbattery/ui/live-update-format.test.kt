package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.BatteryState
import org.junit.Assert.assertEquals
import org.junit.Test

private const val NOW_MS = 10_000_000_000L
private const val MINUTE_MS = 60_000L
private const val MODEL = "Realme Buds T310"

private fun textOf(reading: BatteryReading): LiveUpdateText {
    val state = BatteryState(BatterySnapshot(reading, savedAtMillis = NOW_MS), connected = true, connectedAtMillis = NOW_MS - MINUTE_MS)
    return liveUpdateTextOf(budsViewOf(state, NOW_MS), MODEL)
}

// Expected strings are the approved preview's, state by state
class LiveUpdateFormatTest {

    @Test
    fun `in the case with the lid open`() {
        val text = textOf(BatteryReading(BatteryLevel(80, true), BatteryLevel(75, true), BatteryLevel(60, false)))

        assertEquals(LiveUpdateText("Left 80%  ·  Right 75%", "Charging  ·  Case 60%", "75%"), text)
    }

    @Test
    fun `both in the ears`() {
        val text = textOf(BatteryReading(BatteryLevel(80, false), BatteryLevel(75, false), case = null))

        assertEquals(LiveUpdateText("Left 80%  ·  Right 75%", "Connected", "75%"), text)
    }

    @Test
    fun `one bud`() {
        val text = textOf(BatteryReading(left = null, right = BatteryLevel(75, false), case = null))

        assertEquals(LiveUpdateText("Right 75%", "Connected", "75%"), text)
    }

    @Test
    fun `low battery warns at 20 percent or less`() {
        val text = textOf(BatteryReading(BatteryLevel(18, false), BatteryLevel(15, false), case = null))

        assertEquals(LiveUpdateText("Left 18%  ·  Right 15%", "Battery low  ·  charge soon", "15%"), text)
    }

    @Test
    fun `just connected shows an active placeholder, never an empty state`() {
        val text = liveUpdateTextOf(budsViewOf(BatteryState(snapshot = null, connected = true), NOW_MS), MODEL)

        assertEquals(LiveUpdateText("Reading battery…", MODEL, chip = null), text)
    }
}
