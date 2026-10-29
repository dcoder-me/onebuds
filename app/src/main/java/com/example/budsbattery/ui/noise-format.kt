package com.example.budsbattery.ui

import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.NoiseMode

private const val READING = "Reading…"
private const val NOT_CONNECTED = "Not connected"

/**
 * The noise mode every surface shows. Only a connected pair has a mode.
 *
 * @param state - saved state
 * @returns the mode the buds report, or null while disconnected or not reported yet
 */
fun noiseModeOf(state: BatteryState): NoiseMode? = state.noiseMode?.takeIf { state.connected }

/**
 * Names a noise mode for people.
 *
 * @param mode - noise mode
 * @returns a short label such as "Noise cancelling"
 */
fun noiseLabel(mode: NoiseMode): String = when (mode) {
    NoiseMode.NOISE_CANCELLING -> "Noise cancelling"
    NoiseMode.OFF -> "Off"
    NoiseMode.TRANSPARENCY -> "Transparency"
}

/**
 * Says in a few words what a noise mode does to outside sound.
 *
 * @param mode - noise mode
 * @returns one short line such as "Blocks outside sound"
 */
fun noiseHint(mode: NoiseMode): String = when (mode) {
    NoiseMode.NOISE_CANCELLING -> "Blocks outside sound"
    NoiseMode.OFF -> "Plain sound, saves battery"
    NoiseMode.TRANSPARENCY -> "Lets outside sound in"
}

/**
 * Builds the noise card's status line.
 *
 * @param state - saved state
 * @returns the mode's name, "Reading…" until the buds report it, or "Not connected"
 */
fun formatNoiseSubtitle(state: BatteryState): String = when {
    !state.connected -> NOT_CONNECTED
    else -> noiseModeOf(state)?.let(::noiseLabel) ?: READING
}

/**
 * Picks the icon of a noise mode.
 *
 * @param mode - noise mode
 * @returns a drawable resource id
 */
fun noiseIcon(mode: NoiseMode): Int = when (mode) {
    NoiseMode.NOISE_CANCELLING -> R.drawable.ic_noise_cancelling
    NoiseMode.OFF -> R.drawable.ic_noise_off
    NoiseMode.TRANSPARENCY -> R.drawable.ic_noise_transparency
}
