package com.example.budsbattery.service

import android.content.Context
import com.example.budsbattery.data.NoiseMode
import com.example.budsbattery.data.readBatteryState
import com.example.budsbattery.ui.nextNoiseMode
import com.example.budsbattery.ui.noiseModeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// === NOTE: === The app screen, widgets, notification and sync share one process, so a plain flow carries a pick to the open channel.
// No replay: a pick made while no channel is open is dropped, and every surface falls back to what the buds report
private val noisePicks = MutableSharedFlow<NoiseMode>(extraBufferCapacity = 1)

/** Noise modes the user picked, for the open control channel to send. */
val noiseModeRequests: Flow<NoiseMode> = noisePicks

/**
 * Asks the connected buds to switch noise mode. The saved state changes once the buds confirm.
 *
 * @param mode - mode the user picked
 */
fun requestNoiseMode(mode: NoiseMode) {
    noisePicks.tryEmit(mode)
}

/**
 * Handles a noise tap from outside the app screen (widget, Live Update), where only the saved state is at hand.
 *
 * @description Does nothing while disconnected or before the buds report a mode, so a tap never guesses.
 * @param context - app context
 * @param mode - mode tapped; null steps to the next mode after the current one
 */
suspend fun pickNoiseMode(context: Context, mode: NoiseMode? = null) {
    val current = noiseModeOf(readBatteryState(context)) ?: return
    val target = mode ?: nextNoiseMode(current)
    if (target != current) requestNoiseMode(target)
}
