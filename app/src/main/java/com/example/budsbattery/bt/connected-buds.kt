package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "ConnectedBuds"
private const val PROXY_TIMEOUT_MS = 3_000L

// Audio profiles a pair of buds uses; checked in this order
private val AUDIO_PROFILES = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)

/**
 * Finds the audio device connected right now, using the official profile proxy.
 *
 * @param context - any context
 * @param adapter - Bluetooth adapter of the phone
 * @returns the connected buds, likely buds first by name, or null when no audio device is connected
 */
suspend fun findConnectedBuds(context: Context, adapter: BluetoothAdapter): PairedDevice? {
    val devices = AUDIO_PROFILES.flatMap { profile -> connectedOnProfile(context, adapter, profile) }
        .distinctBy { it.address }
    return devices.firstOrNull { looksLikeBuds(it.name) } ?: devices.firstOrNull()
}

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
