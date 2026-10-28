package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.ReadSource

private const val SEPARATOR = " · "

// One bud or the case as shown. Live means read during this connection, in the latest reading.
data class PartView(val level: BatteryLevel, val live: Boolean, val age: String?)

// Which picture the tile shows.
enum class BudsShape { ONE_BUD, TWO_BUDS, WITH_CASE }

/**
 * Everything every surface shows, derived once from the saved state.
 *
 * @property waiting - connected, but nothing read yet during this connection
 */
data class BudsView(
    val left: PartView?,
    val right: PartView?,
    val case: PartView?,
    val connected: Boolean,
    val waiting: Boolean,
    val status: String,
    val sourceNote: String?,
) {
    val shape: BudsShape
        get() = when {
            case?.live == true -> BudsShape.WITH_CASE
            (left == null) != (right == null) -> BudsShape.ONE_BUD
            else -> BudsShape.TWO_BUDS
        }
}

/**
 * Turns the saved state into what the screen shows. Values from an earlier connection are never shown as live.
 *
 * @param state - saved values, connection flag and connection start
 * @param nowMillis - current time in epoch milliseconds
 * @returns the view every surface draws from
 */
fun budsViewOf(state: BatteryState, nowMillis: Long): BudsView {
    val snapshot = state.snapshot
    val fresh = snapshot != null && snapshot.savedAtMillis >= (state.connectedAtMillis ?: Long.MIN_VALUE)
    return when {
        snapshot == null -> emptyView(state.connected)
        state.connected && !fresh -> waitingView(snapshot, nowMillis)
        state.connected -> liveView(snapshot, nowMillis)
        else -> pastView(snapshot, nowMillis)
    }
}

private fun emptyView(connected: Boolean) = BudsView(
    left = null,
    right = null,
    case = null,
    connected = connected,
    waiting = connected,
    status = if (connected) "Connected${SEPARATOR}reading…" else "Not connected${SEPARATOR}not read yet",
    sourceNote = null,
)

// Buds change fast, so last connection's values are hidden; the case can only be shown as last seen
private fun waitingView(snapshot: BatterySnapshot, nowMillis: Long) = emptyView(connected = true).copy(
    case = caseView(snapshot, nowMillis, liveAllowed = false),
)

private fun liveView(snapshot: BatterySnapshot, nowMillis: Long) = BudsView(
    left = snapshot.reading.left?.let { PartView(it, live = true, age = null) },
    right = snapshot.reading.right?.let { PartView(it, live = true, age = null) },
    case = caseView(snapshot, nowMillis, liveAllowed = true),
    connected = true,
    waiting = false,
    status = "Connected${SEPARATOR}updated ${formatAge(snapshot.savedAtMillis, nowMillis)}",
    sourceNote = sourceNoteOf(snapshot.source),
)

private fun pastView(snapshot: BatterySnapshot, nowMillis: Long) = BudsView(
    left = snapshot.reading.left?.let { PartView(it, live = false, age = null) },
    right = snapshot.reading.right?.let { PartView(it, live = false, age = null) },
    case = caseView(snapshot, nowMillis, liveAllowed = false),
    connected = false,
    waiting = false,
    status = "Not connected${SEPARATOR}read ${formatAge(snapshot.savedAtMillis, nowMillis)}",
    sourceNote = null,
)

// The case is live only when the latest reading carried it
private fun caseView(snapshot: BatterySnapshot, nowMillis: Long, liveAllowed: Boolean): PartView? {
    val level = snapshot.reading.case ?: return null
    val seenAt = snapshot.caseSeenAtMillis ?: snapshot.savedAtMillis
    val live = liveAllowed && seenAt == snapshot.savedAtMillis
    return PartView(level, live, age = if (live) null else "seen ${formatAge(seenAt, nowMillis)}")
}

private fun sourceNoteOf(source: ReadSource?): String? = when (source) {
    ReadSource.MESSAGE_STREAM -> "Live from the buds"
    ReadSource.VENDOR_CHANNEL -> "Live from the buds' own channel"
    ReadSource.FAST_PAIR_ADVERT -> "From the broadcast${SEPARATOR}updates when the case opens"
    null -> null
}
