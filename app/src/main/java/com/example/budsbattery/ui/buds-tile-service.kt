package com.example.budsbattery.ui

import android.app.PendingIntent
import android.content.Intent
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

/** Quick Settings tile. Greyed out while the buds are away, with the last values and their age. */
class BudsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { renderTile(readBatteryState(applicationContext)) }
    }

    // === NOTE: === Tap opens setup, so a revoked permission has an obvious fix
    override fun onClick() {
        val setup = Intent(this, SetupActivity::class.java)
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, setup, PendingIntent.FLAG_IMMUTABLE))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun renderTile(state: BatteryState) {
        val tile = qsTile ?: return
        tile.state = if (state.connected) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = formatTileSubtitle(state, System.currentTimeMillis())
        tile.updateTile()
    }
}
