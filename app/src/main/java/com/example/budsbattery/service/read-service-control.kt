package com.example.budsbattery.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

private const val TAG = "ReadServiceControl"

/**
 * Tells whether the Bluetooth permission is granted. Android refuses the read service without it.
 *
 * @param context - any context
 * @returns true when BLUETOOTH_CONNECT is granted
 */
fun hasBluetoothPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

/**
 * Starts the battery sync, or asks a running one for fresh values. Logs instead of crashing when Android refuses.
 *
 * @param context - app context
 * @param refresh - true to ask for fresh values right away
 * @param announce - true when the buds just connected, so the card pops up
 */
fun startBatteryRead(context: Context, refresh: Boolean = false, announce: Boolean = false) {
    // === NOTE: === Starting a connectedDevice service without this permission crashes the app on Android 14+
    if (!hasBluetoothPermission(context)) {
        Log.w(TAG, "Bluetooth permission missing, read skipped")
        return
    }
    val intent = Intent(context, BatteryReadService::class.java)
    if (refresh) intent.action = ACTION_REFRESH
    intent.putExtra(EXTRA_ANNOUNCE, announce)
    try {
        context.startForegroundService(intent)
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
