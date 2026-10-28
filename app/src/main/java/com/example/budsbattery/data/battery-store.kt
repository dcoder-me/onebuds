package com.example.budsbattery.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.batteryDataStore by preferencesDataStore(name = "battery")

private const val UNKNOWN_PERCENT = -1

private data class PartKeys(val percent: Preferences.Key<Int>, val charging: Preferences.Key<Boolean>)

private val leftKeys = PartKeys(intPreferencesKey("left_percent"), booleanPreferencesKey("left_charging"))
private val rightKeys = PartKeys(intPreferencesKey("right_percent"), booleanPreferencesKey("right_charging"))
private val caseKeys = PartKeys(intPreferencesKey("case_percent"), booleanPreferencesKey("case_charging"))
private val savedAtKey = longPreferencesKey("saved_at")
private val sourceKey = stringPreferencesKey("source")
private val connectedKey = booleanPreferencesKey("connected")
private val connectedAtKey = longPreferencesKey("connected_at")

/**
 * Reads the last saved values and the connection flag.
 *
 * @param context - app context used to open the DataStore file
 * @returns the saved state; snapshot is null until the first reading is saved
 */
suspend fun readBatteryState(context: Context): BatteryState = batteryStateFlow(context).first()

/**
 * Watches the saved values, so a screen can update as soon as a new reading lands.
 *
 * @param context - app context used to open the DataStore file
 * @returns a flow that emits the state now and after every save
 */
fun batteryStateFlow(context: Context): Flow<BatteryState> = context.batteryDataStore.data.map(::toBatteryState)

private fun toBatteryState(prefs: Preferences): BatteryState = BatteryState(
    snapshot = snapshotOf(prefs),
    connected = prefs[connectedKey] ?: false,
    connectedAtMillis = prefs[connectedAtKey],
)

private fun snapshotOf(prefs: Preferences): BatterySnapshot? = prefs[savedAtKey]?.let { savedAt ->
    BatterySnapshot(
        reading = BatteryReading(
            left = readPart(prefs, leftKeys),
            right = readPart(prefs, rightKeys),
            case = readPart(prefs, caseKeys),
        ),
        savedAtMillis = savedAt,
        source = prefs[sourceKey]?.let { name -> ReadSource.entries.firstOrNull { it.name == name } },
    )
}

/**
 * Saves a reading exactly as sent, in a single write so two routes can't interleave.
 *
 * @description Nothing is kept from before: a part missing from the reading is shown as missing.
 * @param context - app context used to open the DataStore file
 * @param reading - values just read
 * @param source - route that gave the values
 * @param nowMillis - current time in epoch milliseconds
 */
suspend fun saveReading(context: Context, reading: BatteryReading, source: ReadSource, nowMillis: Long) {
    context.batteryDataStore.edit { prefs ->
        writePart(prefs, leftKeys, reading.left)
        writePart(prefs, rightKeys, reading.right)
        writePart(prefs, caseKeys, reading.case)
        prefs[savedAtKey] = nowMillis
        prefs[sourceKey] = source.name
    }
}

/**
 * Stores whether the buds are connected right now. A new connection also stamps its start,
 * so values from the last connection are never shown as live.
 *
 * @param context - app context used to open the DataStore file
 * @param connected - true while the read service is listening
 * @param nowMillis - current time in epoch milliseconds
 */
suspend fun saveConnected(context: Context, connected: Boolean, nowMillis: Long = System.currentTimeMillis()) {
    context.batteryDataStore.edit { prefs ->
        prefs[connectedKey] = connected
        if (connected) prefs[connectedAtKey] = nowMillis
    }
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
