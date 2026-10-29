package com.example.budsbattery.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.example.budsbattery.bt.PairedDevice
import com.example.budsbattery.bt.findConnectedBuds
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow
import com.example.budsbattery.data.isSelectedDevice
import com.example.budsbattery.data.liveUpdateFlow
import com.example.budsbattery.data.saveConnected
import com.example.budsbattery.data.saveSelectedAddress
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BatteryReadService"
private const val DETECT_ATTEMPTS = 6
private const val DETECT_WAIT_MS = 2_000L

/** Intent action that asks a running sync for fresh values now. */
const val ACTION_REFRESH = "com.example.budsbattery.action.REFRESH"

/**
 * Keeps battery in sync for as long as the buds stay connected, under an ongoing notification.
 * Ends when Android reports the buds disconnected.
 */
class BatteryReadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var syncing: Job? = null
    private var budsAddress: String? = null

    // Kept current so a repeat start (tile tap, card open) re-posts real values, not "Reading battery…"
    @Volatile
    private var latestState = BatteryState(snapshot = null, connected = true)

    // startForeground can't suspend, so the switch is kept current here
    @Volatile
    private var liveUpdate = true

    // Stops the sync the moment the buds' Bluetooth link drops
    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (isSelectedDevice(budsAddress, device?.address)) stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // ACL_DISCONNECTED is a protected broadcast only the system can send, so exporting is safe
        registerReceiver(disconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED), RECEIVER_EXPORTED)
        scope.launch { batteryStateFlow(applicationContext).collect { latestState = it } }
        scope.launch { liveUpdateFlow(applicationContext).collect { liveUpdate = it } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createBatteryChannel(this)
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_REFRESH) refreshes.tryEmit(Unit)
        if (syncing?.isActive != true) syncing = scope.launch { syncWhileConnected() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(disconnectReceiver)
        scope.cancel()
        super.onDestroy()
    }

    // A permission revoked after the start check makes startForeground throw; stop quietly instead
    private fun enterForeground(): Boolean = try {
        startForeground(BATTERY_NOTIFICATION_ID, buildBatteryNotification(this, foregroundState(), liveUpdate), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        true
    } catch (error: SecurityException) {
        Log.w(TAG, "Not allowed to run in the foreground", error)
        false
    }

    // === NOTE: === Android needs startForeground on every start, and this used to post a blank state each time,
    // so a tap on the tile wiped the live values until the next reading. A running sync reuses the saved state;
    // a new one shows "Reading battery…", since saved values belong to the last connection
    private fun foregroundState(): BatteryState {
        val state = latestState
        return if (syncing?.isActive == true && state.connected) state else BatteryState(snapshot = null, connected = true)
    }

    // The Bluetooth link comes up a moment before the audio profiles, so look a few times before giving up
    private suspend fun awaitConnectedBuds(adapter: BluetoothAdapter): PairedDevice? {
        repeat(DETECT_ATTEMPTS) { attempt ->
            findConnectedBuds(this, adapter)?.let { return it }
            if (attempt < DETECT_ATTEMPTS - 1) delay(DETECT_WAIT_MS)
        }
        return null
    }

    private suspend fun syncWhileConnected() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        val buds = adapter?.let { awaitConnectedBuds(it) }
        if (adapter == null || buds == null) {
            Log.d(TAG, "No buds connected, nothing to sync")
            stopSelf()
            return
        }
        budsAddress = buds.address
        saveSelectedAddress(this, buds.address)
        saveConnected(this, true)
        refreshSurfaces(applicationContext)
        try {
            runBatterySync(applicationContext, adapter, buds.address, refreshes)
        } finally {
            withContext(NonCancellable) {
                saveConnected(this@BatteryReadService, false)
                refreshSurfaces(this@BatteryReadService)
            }
            stopSelf()
        }
    }
}
