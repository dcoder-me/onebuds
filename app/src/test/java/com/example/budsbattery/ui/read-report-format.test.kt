package com.example.budsbattery.ui

import com.example.budsbattery.bt.ReadReport
import com.example.budsbattery.bt.ReadSource
import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import org.junit.Assert.assertEquals
import org.junit.Test

private val failed = ReadReport(null, null, vendorOffered = null, vendorConnected = false, sawFastPair = false)

class ReadReportFormatTest {

    @Test
    fun `success has no failure text`() {
        val reading = BatteryReading(BatteryLevel(80, false), null, null)

        assertEquals("", formatReadFailure(ReadReport(reading, ReadSource.FAST_PAIR, null, null, true)))
    }

    @Test
    fun `refused channel and no broadcast are both explained`() {
        assertEquals(
            "Vendor channel: buds refused the connection\nFast Pair: no broadcast. Open the case next to the phone.",
            formatReadFailure(failed),
        )
    }

    @Test
    fun `missing service record says the buds don't offer the channel`() {
        val text = formatReadFailure(failed.copy(vendorOffered = false))

        assertEquals("Vendor channel: these buds don't offer it", text.lines().first())
    }

    @Test
    fun `open but silent channel and a battery-less broadcast are told apart`() {
        val text = formatReadFailure(failed.copy(vendorConnected = true, sawFastPair = true))

        assertEquals(
            "Vendor channel: opened, but no battery reply\nFast Pair: broadcast seen, but without battery",
            text,
        )
    }
}
