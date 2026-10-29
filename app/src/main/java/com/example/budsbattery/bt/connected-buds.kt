package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "ConnectedBuds"
private const val PROXY_TIMEOUT_MS = 3_000L

// Audio profiles a pair of buds uses; LE Audio matters for Bluetooth 5.x buds on Android 13+
private val AUDIO_PROFILES = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.LE_AUDIO)

private val BLUETOOTH_OUTPUT_TYPES = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
)

/**
 * Finds the audio device connected right now: Bluetooth profile proxies first, then the phone's audio outputs.
 *
 * @param context - any context
 * @param adapter - Bluetooth adapter of the phone
 * @returns the connected buds, likely buds first by name, or null when no audio device is connected
 */
suspend fun findConnectedBuds(context: Context, adapter: BluetoothAdapter): PairedDevice? {
    // Second official source: some phones keep the profile list empty while audio still routes to the buds
    val devices = (onAudioProfiles(context, adapter) + bluetoothAudioOutputs(context)).distinctBy { it.address.uppercase() }
    Log.d(TAG, "Connected audio devices: $devices")
    return pickBuds(devices)
}

/**
 * Finds the buds by their audio profile state alone, the source Android itself updates before the link drops.
 *
 * @description Used to confirm a link drop. The audio output list can lag a real disconnect, so it is left out.
 * @param context - any context
 * @param adapter - Bluetooth adapter of the phone
 * @returns the buds still connected on an audio profile, or null when none is
 */
suspend fun findProfileConnectedBuds(context: Context, adapter: BluetoothAdapter): PairedDevice? =
    pickBuds(onAudioProfiles(context, adapter).distinctBy { it.address.uppercase() })

private fun pickBuds(devices: List<PairedDevice>): PairedDevice? = devices.firstOrNull { looksLikeBuds(it.name) } ?: devices.firstOrNull()

// Asked side by side: one at a time, each silent profile cost its full timeout before the next began
private suspend fun onAudioProfiles(context: Context, adapter: BluetoothAdapter): List<PairedDevice> = coroutineScope {
    AUDIO_PROFILES.map { profile -> async { connectedOnProfile(context, adapter, profile) } }.awaitAll().flatten()
}

private fun bluetoothAudioOutputs(context: Context): List<PairedDevice> =
    context.getSystemService(AudioManager::class.java)
        ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .orEmpty()
        .filter { it.type in BLUETOOTH_OUTPUT_TYPES && it.address.isNotBlank() }
        .map { PairedDevice(name = it.productName?.toString() ?: it.address, address = it.address.uppercase()) }

private suspend fun connectedOnProfile(context: Context, adapter: BluetoothAdapter, profile: Int): List<PairedDevice> =
    withTimeoutOrNull(PROXY_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileId: Int, proxy: BluetoothProfile) {
                    val devices = readConnected(proxy)
                    adapter.closeProfileProxy(profileId, proxy)
                    if (continuation.isActive) continuation.resume(devices)
                }

                override fun onServiceDisconnected(profileId: Int) {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            }
            if (!adapter.getProfileProxy(context, listener, profile)) continuation.resume(emptyList())
        }
    }.orEmpty()

private fun readConnected(proxy: BluetoothProfile): List<PairedDevice> = try {
    proxy.connectedDevices.map(::toPairedDevice)
} catch (error: SecurityException) {
    Log.w(TAG, "Bluetooth permission missing", error)
    emptyList()
}

private fun toPairedDevice(device: BluetoothDevice): PairedDevice =
    PairedDevice(name = device.name ?: device.address, address = device.address)
