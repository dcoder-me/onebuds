package com.example.budsbattery.bt

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothProfile
import com.example.budsbattery.data.isSelectedDevice

/** Profiles that carry media; the buds dropping off one of these means they left. */
val MEDIA_PROFILE_ACTIONS: List<String> = listOf(
    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
    BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED,
)

/** Every audio profile, calls included; any of these connecting is worth a fresh lookup. */
val AUDIO_PROFILE_ACTIONS: List<String> = MEDIA_PROFILE_ACTIONS + BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED

/**
 * Tells whether a profile event says the tracked buds just left.
 *
 * @description The event is trusted over a fresh lookup: Android's audio output list still shows the buds for
 * about half a second after they leave, and a profile lookup can stall for seconds while they disconnect.
 * @param tracked - address of the buds on screen or being synced, or null
 * @param address - device the profile event is about
 * @param state - new profile state from the event
 * @returns true when the tracked buds dropped to disconnected
 */
fun isLeaving(tracked: String?, address: String?, state: Int): Boolean =
    state == BluetoothProfile.STATE_DISCONNECTED && isSelectedDevice(tracked, address)
