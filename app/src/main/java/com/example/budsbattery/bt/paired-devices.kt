package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import android.util.Log

private const val TAG = "PairedDevices"
private val BUDS_NAME_HINTS = listOf("realme", "buds")

/** A device already paired in phone Settings. */
data class PairedDevice(val name: String, val address: String)

/**
 * Lists devices paired in phone Settings, likely buds first.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @returns paired devices, or an empty list when Bluetooth is off or permission is missing
 */
fun listPairedDevices(adapter: BluetoothAdapter): List<PairedDevice> = try {
    adapter.bondedDevices.orEmpty()
        .map { device -> PairedDevice(name = device.name ?: device.address, address = device.address) }
        .sortedWith(compareByDescending<PairedDevice> { looksLikeBuds(it.name) }.thenBy { it.name.lowercase() })
} catch (error: SecurityException) {
    Log.w(TAG, "Bluetooth permission missing", error)
    emptyList()
}

/**
 * Finds the name of a paired device.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address to look up
 * @returns the device name, or null if it is not paired
 */
fun pairedDeviceName(adapter: BluetoothAdapter, address: String): String? =
    listPairedDevices(adapter).find { it.address.equals(address, ignoreCase = true) }?.name

/**
 * Tells whether a device name looks like earbuds, to sort them to the top.
 *
 * @param name - device name from Bluetooth
 * @returns true when the name contains "realme" or "buds"
 */
fun looksLikeBuds(name: String): Boolean = BUDS_NAME_HINTS.any { hint -> name.contains(hint, ignoreCase = true) }
