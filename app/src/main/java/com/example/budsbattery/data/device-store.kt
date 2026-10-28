package com.example.budsbattery.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.deviceDataStore by preferencesDataStore(name = "device")

private val selectedAddressKey = stringPreferencesKey("selected_address")

/**
 * Reads the buds chosen during setup.
 *
 * @param context - app context used to open the DataStore file
 * @returns the MAC address, or null before setup
 */
suspend fun readSelectedAddress(context: Context): String? =
    context.deviceDataStore.data.first()[selectedAddressKey]

/**
 * Saves the buds chosen during setup. Only this device is ever read.
 *
 * @param context - app context used to open the DataStore file
 * @param address - MAC address from the companion association
 */
suspend fun saveSelectedAddress(context: Context, address: String) {
    context.deviceDataStore.edit { prefs -> prefs[selectedAddressKey] = address }
}

/**
 * Checks that an event came from the chosen buds. MAC case does not matter.
 *
 * @param selected - the saved MAC address, or null before setup
 * @param address - the MAC address in the event
 * @returns true only when both are set and equal
 */
fun isSelectedDevice(selected: String?, address: String?): Boolean =
    selected != null && address != null && selected.equals(address, ignoreCase = true)
