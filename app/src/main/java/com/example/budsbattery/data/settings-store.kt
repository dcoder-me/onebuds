package com.example.budsbattery.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val liveUpdateKey = booleanPreferencesKey("live_update")

/**
 * Watches the Live Update switch. On by default.
 *
 * @param context - app context used to open the DataStore file
 * @returns a flow that emits the switch now and after every change
 */
fun liveUpdateFlow(context: Context): Flow<Boolean> = context.settingsDataStore.data.map { it[liveUpdateKey] ?: true }

/**
 * Reads the Live Update switch once.
 *
 * @param context - app context used to open the DataStore file
 * @returns true when the battery notification should ask to be pinned at the top
 */
suspend fun readLiveUpdate(context: Context): Boolean = liveUpdateFlow(context).first()

/**
 * Saves the Live Update switch.
 *
 * @param context - app context used to open the DataStore file
 * @param enabled - true to ask Android to pin the notification at the top with the status-bar pill
 */
suspend fun saveLiveUpdate(context: Context, enabled: Boolean) {
    context.settingsDataStore.edit { prefs -> prefs[liveUpdateKey] = enabled }
}
