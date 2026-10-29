package com.example.budsbattery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadSourceRankTest {

    @Test
    fun `anything is shown while no route is live`() {
        assertTrue(shouldAccept(liveRoutes = emptySet(), incoming = ReadSource.FAST_PAIR_ADVERT))
    }

    @Test
    fun `the broadcast is ignored while the buds answer directly`() {
        assertFalse(shouldAccept(setOf(ReadSource.MESSAGE_STREAM), ReadSource.FAST_PAIR_ADVERT))
        assertFalse(shouldAccept(setOf(ReadSource.VENDOR_CHANNEL), ReadSource.FAST_PAIR_ADVERT))
    }

    @Test
    fun `only the best live route feeds values, so two routes never take turns`() {
        val both = setOf(ReadSource.VENDOR_CHANNEL, ReadSource.MESSAGE_STREAM)

        assertTrue(shouldAccept(both, ReadSource.VENDOR_CHANNEL))
        assertFalse(shouldAccept(both, ReadSource.MESSAGE_STREAM))
    }

    @Test
    fun `the next route takes over the moment the best one closes`() {
        assertTrue(shouldAccept(setOf(ReadSource.MESSAGE_STREAM), ReadSource.MESSAGE_STREAM))
        assertTrue(shouldAccept(emptySet(), ReadSource.FAST_PAIR_ADVERT))
    }
}
