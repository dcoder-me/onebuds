package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryState

private const val SEPARATOR = " · "
private const val CHIP_BOLT = "⚡"
private const val CHARGING_MARK = " ⚡"
private const val NOT_READ_YET = "Not read yet"
private const val READING = "Reading…"
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/**
 * Builds the buds part of a line. Missing buds are left out, so one bud reads "R 75".
 *
 * @param view - what the screen shows
 * @returns text such as "L 80 · R 75 ⚡", or an empty string when no bud is known
 */
fun formatBudsLine(view: BudsView): String = listOfNotNull(
    view.left?.let { formatPart("L", it.level) },
    view.right?.let { formatPart("R", it.level) },
).joinToString(SEPARATOR)

/**
 * Builds the tiny status-bar chip text of the Live Update: the case while a bud sits in it, else the bud that runs out first.
 *
 * @description The chip is at most 96dp and shows full text only under 7 characters, so one number and a bolt fit.
 * @param view - what the screen shows
 * @returns text such as "75%" or "⚡60%", or an empty string when nothing is live
 */
fun formatChipText(view: BudsView): String {
    val part = view.focus?.takeIf { it.live } ?: return ""
    val bolt = if (view.focusCharging) CHIP_BOLT else ""
    return "$bolt${part.level.percent}%"
}

/**
 * Builds the tile subtitle from the shared view.
 *
 * @param state - saved values and connection flag
 * @param nowMillis - current time in epoch milliseconds
 * @returns "L 80 · R 75 · Case 60" while live, "Reading…" while waiting, or the last values with their age
 */
fun formatTileSubtitle(state: BatteryState, nowMillis: Long): String {
    val view = budsViewOf(state, nowMillis)
    if (view.waiting) return READING
    val buds = formatBudsLine(view)
    if (buds.isEmpty()) return NOT_READ_YET
    if (!view.connected) return "$buds$SEPARATOR${formatAge(state.snapshot?.savedAtMillis ?: nowMillis, nowMillis)}"
    val case = view.case?.takeIf { it.live }?.let { formatPart("Case", it.level) }
    return listOfNotNull(buds, case).joinToString(SEPARATOR)
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
