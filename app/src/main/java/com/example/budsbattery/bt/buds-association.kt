package com.example.budsbattery.bt

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.budsbattery.data.saveSelectedAddress

private const val TAG = "BudsAssociation"

/**
 * Builds the request that links the app to one paired device, so Android wakes the app when it connects.
 *
 * @param address - MAC address of the buds the user picked
 * @returns a request that matches only that device
 */
// === NOTE: === Without an address filter, single-device mode grabs the first random device nearby
fun buildBudsAssociationRequest(address: String): AssociationRequest = AssociationRequest.Builder()
    .addDeviceFilter(BluetoothDeviceFilter.Builder().setAddress(address).build())
    .setSingleDevice(true)
    .build()

/**
 * Saves the chosen buds and asks the system to wake the app when they connect or disconnect.
 *
 * @param context - app context
 * @param info - association the system created for the picked device
 * @returns the saved MAC address, or null if the association has none
 * @throws android.companion.DeviceNotAssociatedException if the system no longer knows the device
 */
suspend fun saveAssociation(context: Context, info: AssociationInfo): String? {
    val address = associationAddress(info) ?: return null
    saveSelectedAddress(context, address)
    observePresence(context.getSystemService(CompanionDeviceManager::class.java), info, address)
    return address
}

// The wake-up is a bonus: if Android refuses it, the ACL receiver still starts reads, so never crash here
private fun observePresence(manager: CompanionDeviceManager, info: AssociationInfo, address: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            manager.startObservingDevicePresence(
                ObservingDevicePresenceRequest.Builder().setAssociationId(info.id).build(),
            )
        } else {
            @Suppress("DEPRECATION")
            manager.startObservingDevicePresence(address)
        }
    } catch (error: RuntimeException) {
        Log.w(TAG, "Presence wake-up not available", error)
    }
}

/**
 * Reads the MAC address out of an association.
 *
 * @param info - association from the system
 * @returns the address as "AA:BB:CC:DD:EE:FF", or null if the association has none
 */
fun associationAddress(info: AssociationInfo): String? = info.deviceMacAddress?.toString()
