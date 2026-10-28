package com.example.budsbattery.bt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairedDevicesTest {

    @Test
    fun `buds names are spotted in any case`() {
        assertTrue(looksLikeBuds("realme Buds T310"))
        assertTrue(looksLikeBuds("REALME T310"))
        assertTrue(looksLikeBuds("Galaxy Buds2"))
    }

    @Test
    fun `other devices are not taken for buds`() {
        assertFalse(looksLikeBuds("Car Audio"))
        assertFalse(looksLikeBuds("OnePlus Watch"))
    }
}
