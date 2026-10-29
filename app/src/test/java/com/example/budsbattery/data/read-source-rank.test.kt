package com.example.budsbattery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadSourceRankTest {

    @Test
    fun `the broadcast is ignored once the buds answer directly`() {
        assertTrue(shouldAccept(bestSoFar = null, incoming = ReadSource.FAST_PAIR_ADVERT))
        assertTrue(shouldAccept(ReadSource.FAST_PAIR_ADVERT, ReadSource.MESSAGE_STREAM))
        assertFalse(shouldAccept(ReadSource.VENDOR_CHANNEL, ReadSource.FAST_PAIR_ADVERT))
    }

    @Test
    fun `both direct routes stay live, so one going quiet never freezes values`() {
        assertTrue(shouldAccept(ReadSource.MESSAGE_STREAM, ReadSource.VENDOR_CHANNEL))
        assertTrue(shouldAccept(ReadSource.VENDOR_CHANNEL, ReadSource.MESSAGE_STREAM))
    }
}
