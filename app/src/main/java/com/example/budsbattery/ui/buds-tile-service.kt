package com.example.budsbattery.ui

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.readBatteryState
import com.example.budsbattery.service.startBatteryRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile. Its icon shows one bud, two buds, or buds with the case; tapping opens the battery boxes
 * over the panel.
 *
 * @description Android tiles only hold an icon and two text lines, so the rings live in a card the tile shows.
 * Not an "active" tile: Android binds it each time the panel opens.
 */
class BudsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { renderTile(readBatteryState(applicationContext)) }
    }

    // Fresh values are asked for as the card opens, so the rings fill to the current level
    override fun onClick() {
        startBatteryRead(applicationContext, refresh = true)
        showPanelCard { BatteryBoxesCard() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun renderTile(state: BatteryState) {
        val tile = qsTile ?: return
        val view = budsViewOf(state, System.currentTimeMillis())
        // INACTIVE, not UNAVAILABLE: an unavailable tile ignores taps
        tile.state = if (view.connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, shapeIcon(view.shape))
        tile.label = getString(R.string.tile_label)
        tile.subtitle = formatTileSubtitle(state, System.currentTimeMillis())
        tile.updateTile()
    }
}
