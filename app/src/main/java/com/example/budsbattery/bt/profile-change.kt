package com.example.budsbattery.bt

import android.bluetooth.BluetoothProfile
import com.example.budsbattery.data.isSelectedDevice

/**
 * Tells whether a profile event says the buds on screen just left.
 *
 * @description The event is trusted over a fresh lookup: Android's audio output list still shows the buds for
 * about half a second after they leave, so a lookup on the disconnect event kept them on screen.
 * @param shown - buds on screen right now, or null
 * @param address - device the profile event is about
 * @param state - new profile state from the event
 * @returns true when the shown buds dropped to disconnected
 */
fun isLeaving(shown: PairedDevice?, address: String?, state: Int): Boolean =
    state == BluetoothProfile.STATE_DISCONNECTED && isSelectedDevice(shown?.address, address)
