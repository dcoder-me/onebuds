package com.example.budsbattery.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.ReadSource
import com.example.budsbattery.data.readBatteryState
import com.example.budsbattery.data.readLiveUpdate
import com.example.budsbattery.data.saveBudInCase
import com.example.budsbattery.data.saveReading
import com.example.budsbattery.ui.BudsTileService
import com.example.budsbattery.ui.BudsWidget
import com.example.budsbattery.ui.updateBatteryNotification

/**
 * Saves a new reading. The running sync service redraws every surface from the saved state on its own.
 *
 * @param context - app context
 * @param reading - values just read from the buds
 * @param source - route that gave the values
 */
suspend fun publishReading(context: Context, reading: BatteryReading, source: ReadSource) {
    saveReading(context, reading, source, System.currentTimeMillis())
}

/**
 * Saves whether a bud sits in the case. The running sync service redraws every surface from the saved state.
 *
 * @param context - app context
 * @param budInCase - true while at least one bud sits in the case
 */
suspend fun publishPlacement(context: Context, budInCase: Boolean) {
    saveBudInCase(context, budInCase)
}

/**
 * Pushes the saved state and the Live Update switch to the tile, the widget and, while connected, the notification.
 *
 * @param context - app context
 */
suspend fun refreshSurfaces(context: Context) {
    val state = readBatteryState(context)
    // The notification goes first: the widget update can take seconds and must never hold it up
    if (state.connected) updateBatteryNotification(context, state, readLiveUpdate(context))
    refreshHomeSurfaces(context)
}

/**
 * Asks the tile and the widget to redraw from the saved state.
 *
 * @param context - app context
 */
suspend fun refreshHomeSurfaces(context: Context) {
    TileService.requestListeningState(context, ComponentName(context, BudsTileService::class.java))
    BudsWidget().updateAll(context)
}
