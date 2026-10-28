package com.example.budsbattery.ui

import android.app.PendingIntent
import android.content.Intent
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
 * Quick Settings tile. Lit while the buds are connected; dim but tappable with the last values when not.
 *
 * @description Not an "active" tile: Android binds it each time the panel opens, so it always shows
 * the saved values. The sync also calls requestListeningState after each new reading.
 */
class BudsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { renderTile(readBatteryState(applicationContext)) }
    }

    // Connected: ask the sync for fresh values. Not connected: open the app, where the fix is
    override fun onClick() {
        scope.launch {
            if (readBatteryState(applicationContext).connected) {
                startBatteryRead(applicationContext, refresh = true)
            } else {
                openSetup()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun openSetup() {
        val setup = Intent(this, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, setup, PendingIntent.FLAG_IMMUTABLE))
    }

    private fun renderTile(state: BatteryState) {
        val tile = qsTile ?: return
        // INACTIVE, not UNAVAILABLE: an unavailable tile ignores taps
        tile.state = if (state.connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = formatTileSubtitle(state, System.currentTimeMillis())
        tile.updateTile()
    }
}
