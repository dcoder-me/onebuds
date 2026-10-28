package com.example.budsbattery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EARLIER_MS = 1_000L
private const val NOW_MS = 5_000L

private val case60 = BatteryLevel(60, charging = false)
private val previous = BatterySnapshot(
    reading = BatteryReading(BatteryLevel(90, false), BatteryLevel(88, false), case60),
    savedAtMillis = EARLIER_MS,
    caseSeenAtMillis = EARLIER_MS,
    source = ReadSource.MESSAGE_STREAM,
)

class BatteryMergeTest {

    @Test
    fun `a reading without the case drops the case, like AirPods`() {
        val incoming = BatteryReading(BatteryLevel(80, false), BatteryLevel(79, false), case = null)

        val merged = mergeSnapshot(previous, incoming, ReadSource.MESSAGE_STREAM, NOW_MS)

        assertNull(merged.reading.case)
        assertEquals(NOW_MS, merged.savedAtMillis)
    }

    @Test
    fun `a reading with the case updates the case time`() {
        val incoming = BatteryReading(BatteryLevel(80, false), BatteryLevel(79, false), BatteryLevel(55, true))

        val merged = mergeSnapshot(previous, incoming, ReadSource.VENDOR_CHANNEL, NOW_MS)

        assertEquals(BatteryLevel(55, true), merged.reading.case)
        assertEquals(NOW_MS, merged.caseSeenAtMillis)
        assertEquals(ReadSource.VENDOR_CHANNEL, merged.source)
    }

    @Test
    fun `a missing bud stays missing, so one bud in the ear shows as one bud`() {
        val incoming = BatteryReading(BatteryLevel(80, false), right = null, case = null)

        val merged = mergeSnapshot(previous, incoming, ReadSource.MESSAGE_STREAM, NOW_MS)

        assertNull(merged.reading.right)
    }

    @Test
    fun `first reading ever works without a previous snapshot`() {
        val incoming = BatteryReading(BatteryLevel(80, false), null, null)

        val merged = mergeSnapshot(null, incoming, ReadSource.FAST_PAIR_ADVERT, NOW_MS)

        assertNull(merged.reading.case)
        assertNull(merged.caseSeenAtMillis)
    }

    @Test
    fun `a weaker route is ignored once a stronger one has answered`() {
        assertTrue(shouldAccept(bestSoFar = null, incoming = ReadSource.FAST_PAIR_ADVERT))
        assertTrue(shouldAccept(ReadSource.FAST_PAIR_ADVERT, ReadSource.MESSAGE_STREAM))
        assertTrue(shouldAccept(ReadSource.VENDOR_CHANNEL, ReadSource.VENDOR_CHANNEL))
        assertFalse(shouldAccept(ReadSource.VENDOR_CHANNEL, ReadSource.FAST_PAIR_ADVERT))
        assertFalse(shouldAccept(ReadSource.MESSAGE_STREAM, ReadSource.VENDOR_CHANNEL))
    }
}
