package com.example.budsbattery.ui

import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.NoiseMode

// Smallest width in dp that counts as 2, 3 and 4 cells, and the height that counts as 2 rows
private val COLUMN_STEPS = listOf(110f, 180f, 250f)
private const val TWO_ROWS = 110f

private const val NOT_CONNECTED = "Not connected"
private const val IN_CASE = "Buds in case"
private const val READING = "Reading…"

/** A widget's size in home screen cells. */
data class WidgetGrid(val columns: Int, val rows: Int)

/** Layouts of the battery + noise widget, one per approved size. */
enum class BudsLayout { RING, RING_BUTTON, PAIR_BUTTON, TRIO_CHIP, SQUARE, WIDE }

/** Layouts of the battery-only widget. */
enum class BatteryLayout { RING, TILES, BARS, TALL_TILES }

/** Layouts of the noise-only widget. */
enum class NoiseLayout { CYCLE, MODE, SWITCHES }

/**
 * What a widget's noise controls show and whether a tap does anything.
 *
 * @property mode - active mode, null until the buds report one
 * @property note - why the controls are off, shown in their place
 */
data class NoiseControl(val mode: NoiseMode?, val enabled: Boolean, val note: String?)

/**
 * Turns the size the launcher gives a widget into cells.
 *
 * @param widthDp - widget width in dp
 * @param heightDp - widget height in dp
 * @returns columns 1 to 4 and rows 1 to 2
 */
fun widgetGridOf(widthDp: Float, heightDp: Float): WidgetGrid =
    WidgetGrid(columns = 1 + COLUMN_STEPS.count { widthDp >= it }, rows = if (heightDp >= TWO_ROWS) 2 else 1)

/**
 * Picks the battery + noise layout for a size.
 *
 * @param grid - widget size in cells
 * @returns the layout from the approved size table; 3×2 uses the 2×2 layout
 */
fun budsLayoutOf(grid: WidgetGrid): BudsLayout = when {
    grid.columns == 1 -> BudsLayout.RING
    grid.rows == 2 -> if (grid.columns >= 4) BudsLayout.WIDE else BudsLayout.SQUARE
    grid.columns == 2 -> BudsLayout.RING_BUTTON
    grid.columns == 3 -> BudsLayout.PAIR_BUTTON
    else -> BudsLayout.TRIO_CHIP
}

/**
 * Picks the battery-only layout for a size.
 *
 * @param grid - widget size in cells
 * @returns one ring, short tiles, bars for a 4-wide row, or tall tiles for two rows
 */
fun batteryLayoutOf(grid: WidgetGrid): BatteryLayout = when {
    grid.columns == 1 -> BatteryLayout.RING
    grid.rows == 2 -> BatteryLayout.TALL_TILES
    grid.columns >= 4 -> BatteryLayout.BARS
    else -> BatteryLayout.TILES
}

/**
 * Picks the noise-only layout for a size.
 *
 * @param grid - widget size in cells
 * @returns one cycling button, the mode with its name, or all three switches
 */
fun noiseLayoutOf(grid: WidgetGrid): NoiseLayout = when (grid.columns) {
    1 -> NoiseLayout.CYCLE
    2 -> NoiseLayout.MODE
    else -> NoiseLayout.SWITCHES
}

/**
 * Works out the widget's noise controls from the saved state.
 *
 * @description Off while disconnected, while a bud sits in the case, and until the buds report a mode.
 * @param state - saved state
 * @param nowMillis - current time in epoch milliseconds
 * @returns mode, whether taps work, and the note to show when they don't
 */
fun noiseControlOf(state: BatteryState, nowMillis: Long): NoiseControl {
    val mode = noiseModeOf(state)
    val note = when {
        !state.connected -> NOT_CONNECTED
        budsViewOf(state, nowMillis).case != null -> IN_CASE
        mode == null -> READING
        else -> null
    }
    return NoiseControl(mode, enabled = note == null, note = note)
}
