package com.example.budsbattery.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.example.budsbattery.R
import com.example.budsbattery.bt.connectToBuds
import com.example.budsbattery.bt.runSessionWithTimeout
import com.example.budsbattery.data.readSelectedAddress
import com.example.budsbattery.data.saveConnected
import com.example.budsbattery.ui.BATTERY_NOTIFICATION_ID
import com.example.budsbattery.ui.buildBatteryNotification
import com.example.budsbattery.ui.createBatteryChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BatteryReadService"

// Wait after connect, then back off 1s, 2s, 4s between retries
private val ATTEMPT_WAITS_MS = listOf(1_500L, 1_000L, 2_000L, 4_000L)

/** Short foreground service that reads the buds while they are connected. Stops itself when done. */
class BatteryReadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listening: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createBatteryChannel(this)
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (listening?.isActive != true) listening = scope.launch { listenUntilGone() }
        return START_NOT_STICKY
    }

    // A permission revoked after the start check makes startForeground throw; stop quietly instead
    private fun enterForeground(): Boolean = try {
        startForeground(
            BATTERY_NOTIFICATION_ID,
            buildBatteryNotification(this, getString(R.string.reading_battery)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        true
    } catch (error: SecurityException) {
        Log.w(TAG, "Not allowed to run in the foreground", error)
        false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun listenUntilGone() {
        val address = readSelectedAddress(this)
        if (address == null) {
            stopSelf()
            return
        }
        saveConnected(this, true)
        try {
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter != null) connectUntilHeard(adapter, address)
        } finally {
            withContext(NonCancellable) {
                saveConnected(this@BatteryReadService, false)
                refreshSurfaces(this@BatteryReadService)
            }
            stopSelf()
        }
    }

    // Returns once a session has heard the buds, or after the last retry fails
    private suspend fun connectUntilHeard(adapter: BluetoothAdapter, address: String) {
        for (wait in ATTEMPT_WAITS_MS) {
            delay(wait)
            val socket = connectToBuds(adapter, address) ?: continue
            val heard = runSessionWithTimeout(socket) { reading -> publishReading(applicationContext, reading) }
            if (heard) return
        }
    }
}
