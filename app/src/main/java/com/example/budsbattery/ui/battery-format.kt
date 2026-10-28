package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatteryState

private const val SEPARATOR = " · "
private const val CHARGING_MARK = " ⚡"
private const val NOT_READ_YET = "Not read yet"
private const val CONNECTED = "Connected"
private const val NOT_CONNECTED = "Not connected"
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/**
 * Builds the short line shown in the tile and notification. Unknown parts are left out.
 *
 * @param reading - the values to show
 * @returns text such as "L 80 · Case 60 ⚡", or an empty string when nothing is known
 */
fun formatBatteryLine(reading: BatteryReading): String = listOfNotNull(
    reading.left?.let { formatPart("L", it) },
    reading.right?.let { formatPart("R", it) },
    reading.case?.let { formatPart("Case", it) },
).joinToString(SEPARATOR)

/**
 * Builds the tiny status-bar chip text of the Live Update. The chip is at most 96dp wide, so buds only.
 *
 * @param reading - the values to show
 * @returns text such as "L80 R75", or an empty string when neither bud is known
 */
fun formatChipText(reading: BatteryReading): String = listOfNotNull(
    reading.left?.let { "L${it.percent}" },
    reading.right?.let { "R${it.percent}" },
).joinToString(" ")

/**
 * Builds the tile subtitle. Shows the age of old values when the buds are gone.
 *
 * @param state - saved values and connection flag
 * @param nowMillis - current time in epoch milliseconds
 * @returns the subtitle text
 */
fun formatTileSubtitle(state: BatteryState, nowMillis: Long): String {
    val snapshot = state.snapshot ?: return NOT_READ_YET
    val line = formatBatteryLine(snapshot.reading)
    if (line.isEmpty()) return NOT_READ_YET
    if (state.connected) return line
    return "$line$SEPARATOR${formatAge(snapshot.savedAtMillis, nowMillis)}"
}

/**
 * Builds the status line under the battery card in setup.
 *
 * @param state - saved values and connection flag
 * @param nowMillis - current time in epoch milliseconds
 * @returns text such as "Connected · updated just now" or "Not connected · read 2h ago"
 */
fun formatStatusLine(state: BatteryState, nowMillis: Long): String {
    val connection = if (state.connected) CONNECTED else NOT_CONNECTED
    val snapshot = state.snapshot ?: return "$connection$SEPARATOR${NOT_READ_YET.lowercase()}"
    val verb = if (state.connected) "updated" else "read"
    return "$connection$SEPARATOR$verb ${formatAge(snapshot.savedAtMillis, nowMillis)}"
}

/**
 * Turns an elapsed time into a short age such as "just now", "12 min ago" or "2h ago".
 *
 * @param savedAtMillis - when the values were saved, in epoch milliseconds
 * @param nowMillis - current time in epoch milliseconds
 * @returns a short human-readable age
 */
fun formatAge(savedAtMillis: Long, nowMillis: Long): String {
    val elapsed = nowMillis - savedAtMillis
    return when {
        elapsed < MINUTE_MS -> "just now"
        elapsed < HOUR_MS -> "${elapsed / MINUTE_MS} min ago"
        elapsed < DAY_MS -> "${elapsed / HOUR_MS}h ago"
        else -> "${elapsed / DAY_MS}d ago"
    }
}

private fun formatPart(name: String, level: BatteryLevel): String {
    val charging = if (level.charging) CHARGING_MARK else ""
    return "$name ${level.percent}$charging"
}
