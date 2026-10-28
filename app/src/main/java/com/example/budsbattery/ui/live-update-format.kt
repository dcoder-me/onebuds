package com.example.budsbattery.ui

private const val LIVE_SEPARATOR = "  ·  "
private const val READING_TITLE = "Reading battery…"
private const val CONNECTED = "Connected"
private const val CHARGING = "Charging"
private const val LOW_WARNING = "Battery low${LIVE_SEPARATOR}charge soon"

/** The text parts of the Live Update. A null chip leaves the status-bar pill without text. */
data class LiveUpdateText(val title: String, val text: String, val chip: String?)

/**
 * Builds the Live Update text: full-word buds as the title, then the case or a status line.
 *
 * @description While nothing is read yet it says "Reading battery…" (Google: an active placeholder,
 * never an empty state). The case line shows only while the latest reading has the case.
 * @param view - what the screen shows
 * @param modelName - buds name shown under the placeholder, such as "Realme Buds T310"
 * @returns title such as "Left 80%  ·  Right 75%", text such as "Charging  ·  Case 60%", chip such as "75%"
 */
fun liveUpdateTextOf(view: BudsView, modelName: String): LiveUpdateText {
    val buds = listOfNotNull(
        view.left?.let { "Left ${it.level.percent}%" },
        view.right?.let { "Right ${it.level.percent}%" },
    ).joinToString(LIVE_SEPARATOR)
    if (view.waiting || buds.isEmpty()) return LiveUpdateText(READING_TITLE, modelName, chip = null)
    return LiveUpdateText(title = buds, text = statusLineOf(view), chip = formatChipText(view).ifEmpty { null })
}

private fun statusLineOf(view: BudsView): String {
    val case = view.case
    return when {
        case != null -> listOfNotNull(CHARGING.takeIf { view.budsCharging }, "Case ${case.level.percent}%")
            .joinToString(LIVE_SEPARATOR)
        view.low -> LOW_WARNING
        else -> CONNECTED
    }
}
