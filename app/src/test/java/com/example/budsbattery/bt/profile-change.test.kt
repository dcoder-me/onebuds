package com.example.budsbattery.bt

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val BUDS_ADDRESS = "98:47:44:EB:39:1A"

class ProfileChangeTest {

    @Test
    fun `the tracked buds dropping off a profile means they left`() {
        // Address case differs between Android sources
        assertTrue(isLeaving(BUDS_ADDRESS, "98:47:44:eb:39:1a", BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test
    fun `another device leaving does not clear the tracked buds`() {
        assertFalse(isLeaving(BUDS_ADDRESS, "00:11:22:33:44:55", BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test
    fun `the tracked buds connecting is not leaving`() {
        assertFalse(isLeaving(BUDS_ADDRESS, BUDS_ADDRESS, BluetoothProfile.STATE_CONNECTED))
    }

    @Test
    fun `nothing tracked means nothing can leave`() {
        assertFalse(isLeaving(null, BUDS_ADDRESS, BluetoothProfile.STATE_DISCONNECTED))
    }
}
