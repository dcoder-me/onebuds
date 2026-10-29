package com.example.budsbattery.service

import com.example.budsbattery.data.NoiseMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// === NOTE: === The tile, popup and sync share one process, so a plain flow carries a pick to the open channel.
// No replay: a pick made while no channel is open is dropped, and the selector falls back to what the buds report
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
