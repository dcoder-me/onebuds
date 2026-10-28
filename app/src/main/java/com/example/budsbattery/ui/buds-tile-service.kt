package com.example.budsbattery.ui

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.readBatteryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile. Its icon shows one bud, two buds, or buds with the case; tapping opens the AirPods-style card.
 *
 * @description Android tiles only hold an icon and two text lines, so the animated card lives in
 * BudsPopupActivity. Not an "active" tile: Android binds it each time the panel opens.
 */
class BudsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { renderTile(readBatteryState(applicationContext)) }
    }

    // The card asks the sync for fresh values itself, so the tap only needs to open it
    override fun onClick() {
        startActivityAndCollapse(budsPopupIntent(this))
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
