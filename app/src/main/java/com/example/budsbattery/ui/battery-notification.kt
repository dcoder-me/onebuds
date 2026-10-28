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
 * @description Asks Android 16 to promote it to a Live Update (top of the shade, status-bar chip).
 * Android or the phone maker may decline; it then stays a normal pinned notification.
 * @param context - app context
 * @param state - saved values and connection flag
 * @returns the notification to post or hand to startForeground
 */
fun buildBatteryNotification(context: Context, state: BatteryState): Notification {
    val view = budsViewOf(state, System.currentTimeMillis())
    val builder = NotificationCompat.Builder(context, BATTERY_CHANNEL_ID)
        .setSmallIcon(shapeIcon(view.shape))
        .setContentTitle(notificationTitleFor(context, view))
        .setContentText(notificationTextFor(view))
        .setContentIntent(budsPopupIntent(context))
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setRequestPromotedOngoing(true)
    formatChipText(view).takeIf { it.isNotEmpty() }?.let(builder::setShortCriticalText)
    return builder.build()
}

/**
 * Re-posts the battery notification with the latest values. Call only while the buds are connected.
 *
 * @param context - app context
 * @param state - saved values and connection flag
 */
fun updateBatteryNotification(context: Context, state: BatteryState) {
    context.getSystemService(NotificationManager::class.java)
        .notify(BATTERY_NOTIFICATION_ID, buildBatteryNotification(context, state))
}

/**
 * Picks the notification title: the buds, or "Reading battery…" until this connection has values.
 *
 * @param context - app context, used for the fallback text
 * @param view - what the screen shows
 * @returns text such as "L 80 · R 75"
 */
fun notificationTitleFor(context: Context, view: BudsView): String =
    formatBudsLine(view).ifEmpty { context.getString(R.string.reading_battery) }

/**
 * Picks the second line: the case (with its age when old) and where the values come from.
 *
 * @param view - what the screen shows
 * @returns text such as "Case 60 · seen 5 min ago · Live from the buds"
 */
fun notificationTextFor(view: BudsView): String =
    listOf(formatCaseLine(view), view.sourceNote.orEmpty()).filter { it.isNotEmpty() }.joinToString(" · ")
