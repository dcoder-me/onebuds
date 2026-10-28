package com.example.budsbattery.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.BatterySnapshot
import com.example.budsbattery.data.readBatteryState
import com.example.budsbattery.data.saveSnapshot
import com.example.budsbattery.ui.BudsTileService
import com.example.budsbattery.ui.BudsWidget
import com.example.budsbattery.ui.notificationTextFor
import com.example.budsbattery.ui.updateBatteryNotification

/**
 * Saves a new reading, then refreshes the tile, widget and notification from the saved state.
 *
 * @param context - app context
 * @param reading - values just read from the buds
 */
suspend fun publishReading(context: Context, reading: BatteryReading) {
    saveSnapshot(context, BatterySnapshot(reading, savedAtMillis = System.currentTimeMillis()))
    refreshSurfaces(context)
}

/**
 * Pushes the saved state to the tile, the widget and, while connected, the notification.
 *
 * @param context - app context
 */
suspend fun refreshSurfaces(context: Context) {
    val state = readBatteryState(context)
    TileService.requestListeningState(context, ComponentName(context, BudsTileService::class.java))
    BudsWidget().updateAll(context)
    if (state.connected) updateBatteryNotification(context, notificationTextFor(context, state))
}
