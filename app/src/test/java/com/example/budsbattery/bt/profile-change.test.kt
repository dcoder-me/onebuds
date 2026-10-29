package com.example.budsbattery.bt

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val shownBuds = PairedDevice(name = "realme Buds T310", address = "98:47:44:EB:39:1A")

class ProfileChangeTest {

    @Test
    fun `the shown buds dropping off a profile means they left`() {
        // Address case differs between Android sources
        assertTrue(isLeaving(shownBuds, "98:47:44:eb:39:1a", BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test
    fun `another device leaving does not clear the shown buds`() {
        assertFalse(isLeaving(shownBuds, "00:11:22:33:44:55", BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test
    fun `the shown buds connecting is not leaving`() {
        assertFalse(isLeaving(shownBuds, shownBuds.address, BluetoothProfile.STATE_CONNECTED))
    }

    @Test
    fun `nothing shown means nothing can leave`() {
        assertFalse(isLeaving(null, shownBuds.address, BluetoothProfile.STATE_DISCONNECTED))
    }
}
