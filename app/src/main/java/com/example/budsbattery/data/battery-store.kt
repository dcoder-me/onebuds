package com.example.budsbattery.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.batteryDataStore by preferencesDataStore(name = "battery")

private const val UNKNOWN_PERCENT = -1

private data class PartKeys(val percent: Preferences.Key<Int>, val charging: Preferences.Key<Boolean>)

private val leftKeys = PartKeys(intPreferencesKey("left_percent"), booleanPreferencesKey("left_charging"))
private val rightKeys = PartKeys(intPreferencesKey("right_percent"), booleanPreferencesKey("right_charging"))
private val caseKeys = PartKeys(intPreferencesKey("case_percent"), booleanPreferencesKey("case_charging"))
private val savedAtKey = longPreferencesKey("saved_at")
private val connectedKey = booleanPreferencesKey("connected")

/**
 * Reads the last saved values and the connection flag.
 *
 * @param context - app context used to open the DataStore file
 * @returns the saved state; snapshot is null until the first reading is saved
 */
suspend fun readBatteryState(context: Context): BatteryState {
    val prefs = context.batteryDataStore.data.first()
    val snapshot = prefs[savedAtKey]?.let { savedAt ->
        BatterySnapshot(
            reading = BatteryReading(
                left = readPart(prefs, leftKeys),
                right = readPart(prefs, rightKeys),
                case = readPart(prefs, caseKeys),
            ),
            savedAtMillis = savedAt,
        )
    }
    return BatteryState(snapshot = snapshot, connected = prefs[connectedKey] ?: false)
}

/**
 * Saves a reading with its time. Leaves the connection flag alone.
 *
 * @param context - app context used to open the DataStore file
 * @param snapshot - values and the time they were read
 */
suspend fun saveSnapshot(context: Context, snapshot: BatterySnapshot) {
    context.batteryDataStore.edit { prefs ->
        writePart(prefs, leftKeys, snapshot.reading.left)
        writePart(prefs, rightKeys, snapshot.reading.right)
        writePart(prefs, caseKeys, snapshot.reading.case)
        prefs[savedAtKey] = snapshot.savedAtMillis
    }
}

/**
 * Stores whether the buds are connected right now.
 *
 * @param context - app context used to open the DataStore file
 * @param connected - true while the read service is listening
 */
suspend fun saveConnected(context: Context, connected: Boolean) {
    context.batteryDataStore.edit { prefs -> prefs[connectedKey] = connected }
}

private fun readPart(prefs: Preferences, keys: PartKeys): BatteryLevel? {
    val percent = prefs[keys.percent] ?: UNKNOWN_PERCENT
    if (percent == UNKNOWN_PERCENT) return null
    return BatteryLevel(percent, charging = prefs[keys.charging] ?: false)
}

private fun writePart(prefs: MutablePreferences, keys: PartKeys, level: BatteryLevel?) {
    prefs[keys.percent] = level?.percent ?: UNKNOWN_PERCENT
    prefs[keys.charging] = level?.charging ?: false
}
