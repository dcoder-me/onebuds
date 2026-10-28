package com.example.budsbattery.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.budsbattery.bt.connectToBuds
import com.example.budsbattery.bt.fastPairBatteryFlow
import com.example.budsbattery.bt.offersVendorChannel
import com.example.budsbattery.bt.runSessionWithTimeout
import com.example.budsbattery.data.BatteryReading
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

private const val TAG = "BatterySync"

// The buds push changes on their own; this pull is a safety net in case a push is missed
private const val PULL_INTERVAL_MS = 5 * 60_000L

// Wait before each reconnect: quick at first, then at most every 5 minutes
private val RECONNECT_WAITS_MS = listOf(1_500L, 2_000L, 5_000L, 15_000L, 60_000L, 5 * 60_000L)

/**
 * Keeps battery values fresh until cancelled: the buds' own channel (pushes + pulls) and the
 * Fast Pair broadcast run side by side, and whichever has news publishes it.
 *
 * @param context - app context
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the connected buds
 * @param refreshes - each emission asks both routes for fresh values now
 */
suspend fun runBatterySync(context: Context, adapter: BluetoothAdapter, address: String, refreshes: Flow<Unit>): Nothing =
    coroutineScope {
        launch { syncOverVendorChannel(context, adapter, address, refreshes) }
        launch { syncOverFastPair(context, adapter, refreshes) }
        // Either route may give up (not offered, no permission); the sync still lives until the buds disconnect
        awaitCancellation()
    }

private suspend fun syncOverVendorChannel(
    context: Context,
    adapter: BluetoothAdapter,
    address: String,
    refreshes: Flow<Unit>,
) {
    if (offersVendorChannel(adapter, address) == false) {
        Log.d(TAG, "Buds don't offer the vendor channel; Fast Pair only")
        return
    }
    val requests = merge(refreshes, ticker(PULL_INTERVAL_MS))
    var failures = 0
    while (true) {
        delay(RECONNECT_WAITS_MS[failures.coerceAtMost(RECONNECT_WAITS_MS.lastIndex)])
        val socket = connectToBuds(adapter, address)
        val heard = socket != null && runSessionWithTimeout(socket, requests) { publishReading(context, it) }
        failures = if (heard) 0 else failures + 1
    }
}

private suspend fun syncOverFastPair(context: Context, adapter: BluetoothAdapter, refreshes: Flow<Unit>) =
    coroutineScope {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No scan permission; vendor channel only")
            return@coroutineScope
        }
        // Adverts repeat many times a second; only a change (or a refresh request) is published
        val lastPublished = AtomicReference<BatteryReading?>(null)
        launch { refreshes.collect { lastPublished.set(null) } }
        fastPairBatteryFlow(adapter).collect { reading ->
            if (lastPublished.getAndSet(reading) != reading) publishReading(context, reading)
        }
    }

private fun ticker(intervalMs: Long): Flow<Unit> = flow {
    while (true) {
        delay(intervalMs)
        emit(Unit)
    }
}
