package com.example.budsbattery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadSourceRankTest {

    @Test
    fun `a weaker route is ignored once a stronger one has answered`() {
        assertTrue(shouldAccept(bestSoFar = null, incoming = ReadSource.FAST_PAIR_ADVERT))
        assertTrue(shouldAccept(ReadSource.FAST_PAIR_ADVERT, ReadSource.MESSAGE_STREAM))
        assertTrue(shouldAccept(ReadSource.VENDOR_CHANNEL, ReadSource.VENDOR_CHANNEL))
        assertFalse(shouldAccept(ReadSource.VENDOR_CHANNEL, ReadSource.FAST_PAIR_ADVERT))
        assertFalse(shouldAccept(ReadSource.MESSAGE_STREAM, ReadSource.VENDOR_CHANNEL))
    }
}
