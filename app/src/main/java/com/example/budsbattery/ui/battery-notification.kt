package com.example.budsbattery.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState

const val BATTERY_CHANNEL_ID = "buds-battery"
const val BATTERY_NOTIFICATION_ID = 1

/**
 * Creates the quiet channel for the battery notification. Safe to call again.
 *
 * @param context - app context
 */
fun createBatteryChannel(context: Context) {
    val channel = NotificationChannel(
        BATTERY_CHANNEL_ID,
        context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_LOW,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/**
 * Builds the silent, ongoing notification that the read service runs under.
 *
 * @param context - app context
 * @param text - the line to show, such as "L 80 · Case 60"
 * @returns the notification to post or hand to startForeground
 */
fun buildBatteryNotification(context: Context, text: String): Notification =
    Notification.Builder(context, BATTERY_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_buds)
        .setContentTitle(context.getString(R.string.tile_label))
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()

/**
 * Replaces the text of the battery notification. Call only while the buds are connected.
 *
 * @param context - app context
 * @param text - the new line to show
 */
fun updateBatteryNotification(context: Context, text: String) {
    val notification = buildBatteryNotification(context, text)
    context.getSystemService(NotificationManager::class.java).notify(BATTERY_NOTIFICATION_ID, notification)
}

/**
 * Picks the notification text for the current state.
 *
 * @param context - app context, used for the fallback text
 * @param state - saved values and connection flag
 * @returns the battery line, or "Reading battery…" style fallback when nothing is known
 */
fun notificationTextFor(context: Context, state: BatteryState): String {
    val line = state.snapshot?.let { formatBatteryLine(it.reading) }.orEmpty()
    return line.ifEmpty { context.getString(R.string.reading_battery) }
}
