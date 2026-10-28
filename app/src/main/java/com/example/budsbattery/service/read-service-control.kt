package com.example.budsbattery.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "ReadServiceControl"

/**
 * Starts the battery read service. Logs instead of crashing when Android refuses the start.
 *
 * @param context - app context
 */
fun startBatteryRead(context: Context) {
    try {
        context.startForegroundService(Intent(context, BatteryReadService::class.java))
    } catch (error: ForegroundServiceStartNotAllowedException) {
        Log.w(TAG, "Android blocked the background start", error)
    }
}

/**
 * Stops the battery read service. Its own cleanup marks the buds as disconnected.
 *
 * @param context - app context
 */
fun stopBatteryRead(context: Context) {
    context.stopService(Intent(context, BatteryReadService::class.java))
}
