package com.example.budsbattery.ui

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.NoiseMode
import com.example.budsbattery.data.readBatteryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile for noise control. Its icon and subtitle show the buds' mode; tapping opens the mode pill
 * over the panel.
 */
class NoiseTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { renderTile(readBatteryState(applicationContext)) }
    }

    override fun onClick() {
        showPanelCard { NoiseCard() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun renderTile(state: BatteryState) {
        val tile = qsTile ?: return
        val mode = noiseModeOf(state)
        // Lit while the buds shape the sound; off and unknown stay grey
        tile.state = if (mode != null && mode != NoiseMode.OFF) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, noiseIcon(mode ?: NoiseMode.NOISE_CANCELLING))
        tile.label = getString(R.string.noise_tile_label)
        tile.subtitle = formatNoiseSubtitle(state)
        tile.updateTile()
    }
}
