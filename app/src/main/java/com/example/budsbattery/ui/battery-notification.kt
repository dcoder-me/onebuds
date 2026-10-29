package com.example.budsbattery.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState

const val BATTERY_NOTIFICATION_ID = 1

// === NOTE: === Channel importance can't change after creation, so the louder-placed channel needs a new ID
private const val BATTERY_CHANNEL_ID = "buds-battery-live"
private const val OLD_CHANNEL_ID = "buds-battery"

/**
 * Creates the battery channel: shown in the main section (not "Silent"), but with no sound or vibration.
 *
 * @param context - app context
 */
fun createBatteryChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.deleteNotificationChannel(OLD_CHANNEL_ID)
    val channel = NotificationChannel(
        BATTERY_CHANNEL_ID,
        context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        setSound(null, null)
        enableVibration(false)
        setShowBadge(false)
    }
    manager.createNotificationChannel(channel)
}

/**
 * Builds the ongoing battery notification the sync service runs under.
 *
 * @description Asks Android 16 to promote it to a Live Update (top of the shade, status-bar pill).
 * Only promotable parts are used: text, small icon, a drawn large icon and an accent colour, no custom layout.
 * Android or the phone maker may decline; it then stays a normal pinned notification.
 * @param context - app context
 * @param state - saved values and connection flag
 * @param liveUpdate - the app's Live Update switch; off leaves a plain ongoing notification
 * @returns the notification to post or hand to startForeground
 */
fun buildBatteryNotification(context: Context, state: BatteryState, liveUpdate: Boolean): Notification {
    val view = budsViewOf(state, System.currentTimeMillis())
    val modelName = context.getString(R.string.app_subtitle)
    val live = liveUpdateTextOf(view, modelName)
    val builder = NotificationCompat.Builder(context, BATTERY_CHANNEL_ID)
        .setSmallIcon(shapeIcon(view.shape))
        .setContentTitle(live.title)
        .setContentText(live.text)
        .setSubText(modelName)
        .setLargeIcon(liveUpdateIcon(context, view))
        .setColor(liveAccentColor(context, view.low))
        .setContentIntent(budsPopupIntent(context))
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setRequestPromotedOngoing(liveUpdate)
    live.chip?.let(builder::setShortCriticalText)
    return builder.build()
}

/**
 * Re-posts the battery notification with the latest values. Call only while the buds are connected.
 *
 * @param context - app context
 * @param state - saved values and connection flag
 * @param liveUpdate - the app's Live Update switch
 */
fun updateBatteryNotification(context: Context, state: BatteryState, liveUpdate: Boolean) {
    context.getSystemService(NotificationManager::class.java)
        .notify(BATTERY_NOTIFICATION_ID, buildBatteryNotification(context, state, liveUpdate))
}
