package com.example.budsbattery.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    val reading = state.snapshot?.reading
    val builder = NotificationCompat.Builder(context, BATTERY_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_buds)
        .setContentTitle(context.getString(R.string.notification_title))
        .setContentText(notificationTextFor(context, state))
        .setContentIntent(openSetupIntent(context))
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setRequestPromotedOngoing(true)
    reading?.let { formatChipText(it) }?.takeIf { it.isNotEmpty() }?.let(builder::setShortCriticalText)
    state.snapshot?.let { builder.setWhen(it.savedAtMillis).setShowWhen(true) }
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
 * Picks the notification text for the current state.
 *
 * @param context - app context, used for the fallback text
 * @param state - saved values and connection flag
 * @returns the battery line, or "Reading battery…" when nothing is known
 */
fun notificationTextFor(context: Context, state: BatteryState): String {
    val line = state.snapshot?.let { formatBatteryLine(it.reading) }.orEmpty()
    return line.ifEmpty { context.getString(R.string.reading_battery) }
}

private fun openSetupIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_IMMUTABLE,
)
