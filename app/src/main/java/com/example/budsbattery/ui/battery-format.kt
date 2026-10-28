package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatteryState

private const val SEPARATOR = " · "
private const val CHARGING_MARK = " ⚡"
private const val NOT_READ_YET = "Not read yet"
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
