package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.ReadSource

private const val SEPARATOR = " · "

/** At or below this a part shows in red. */
const val LOW_PERCENT = 20

// One bud or the case as shown. Live means read during this connection.
data class PartView(val level: BatteryLevel, val live: Boolean)

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

    // The bud that runs out first, so the chip and the ring show it
    val lowerBud: PartView?
        get() = listOfNotNull(left, right).minByOrNull { it.level.percent }

    val low: Boolean
        get() = lowerBud?.let { it.level.percent <= LOW_PERCENT } == true

    val budsCharging: Boolean
        get() = left?.level?.charging == true || right?.level?.charging == true
}

/**
 * Turns the saved state into what the screen shows. Values from an earlier connection are never shown as live,
 * and the case shows only when the latest reading has it.
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
        // Buds and case change fast, so last connection's values are hidden until this one answers
        state.connected && !fresh -> emptyView(connected = true)
        state.connected -> liveView(snapshot)
        else -> pastView(snapshot, nowMillis)
    }
}

private fun emptyView(connected: Boolean) = BudsView(
    left = null,
    right = null,
    case = null,
    connected = connected,
    waiting = connected,
    status = if (connected) "Connecting…" else "Not connected${SEPARATOR}not read yet",
    sourceNote = null,
)

private fun liveView(snapshot: BatterySnapshot) = partsOf(snapshot, live = true).copy(
    connected = true,
    status = "Connected",
    sourceNote = sourceNoteOf(snapshot.source),
)

private fun pastView(snapshot: BatterySnapshot, nowMillis: Long) = partsOf(snapshot, live = false).copy(
    status = "Not connected${SEPARATOR}read ${formatAge(snapshot.savedAtMillis, nowMillis)}",
)

private fun partsOf(snapshot: BatterySnapshot, live: Boolean) = BudsView(
    left = snapshot.reading.left?.let { PartView(it, live) },
    right = snapshot.reading.right?.let { PartView(it, live) },
    case = snapshot.reading.case?.let { PartView(it, live) },
    connected = false,
    waiting = false,
    status = "",
    sourceNote = null,
)

private fun sourceNoteOf(source: ReadSource?): String? = when (source) {
    ReadSource.MESSAGE_STREAM -> "Live from the buds"
    ReadSource.VENDOR_CHANNEL -> "Live from the buds' own channel"
    ReadSource.FAST_PAIR_ADVERT -> "From the broadcast${SEPARATOR}updates when the case opens"
    null -> null
}
