package com.example.budsbattery.bt

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import com.example.budsbattery.data.saveSelectedAddress

/**
 * Builds the request that shows the system's list of nearby Bluetooth devices.
 *
 * @returns a request that lets the user pick one device
 */
fun buildBudsAssociationRequest(): AssociationRequest = AssociationRequest.Builder()
    .addDeviceFilter(BluetoothDeviceFilter.Builder().build())
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
    context.getSystemService(CompanionDeviceManager::class.java).startObservingDevicePresence(address)
    return address
}

/**
 * Reads the MAC address out of an association.
 *
 * @param info - association from the system
 * @returns the address as "AA:BB:CC:DD:EE:FF", or null if the association has none
 */
fun associationAddress(info: AssociationInfo): String? = info.deviceMacAddress?.toString()
