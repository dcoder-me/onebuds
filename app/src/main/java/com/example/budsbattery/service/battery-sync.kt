package com.example.budsbattery.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.budsbattery.bt.connectMessageStream
import com.example.budsbattery.bt.connectToBuds
import com.example.budsbattery.bt.fastPairBatteryFlow
import com.example.budsbattery.bt.offersVendorChannel
import com.example.budsbattery.bt.runMessageStream
import com.example.budsbattery.bt.runSessionWithTimeout
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.ReadSource
import com.example.budsbattery.data.shouldAccept
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "BatterySync"

// The buds push changes on their own; this pull is a safety net in case a push is missed
private const val PULL_INTERVAL_MS = 60_000L

// Wait before each reconnect: quick at first, then at most every 5 minutes
private val RECONNECT_WAITS_MS = listOf(1_500L, 2_000L, 5_000L, 15_000L, 60_000L, 5 * 60_000L)

// A refused Message Stream usually means Google services hold it; stop trying after this many
private const val MESSAGE_STREAM_MAX_REFUSALS = 3

/**
 * Keeps battery values fresh until cancelled. Three routes run side by side; the one closest to the
 * buds wins and weaker ones are ignored from then on, so values never flip between routes.
 *
 * @param context - app context
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the connected buds
 * @param refreshes - each emission asks the routes for fresh values now
 */
suspend fun runBatterySync(context: Context, adapter: BluetoothAdapter, address: String, refreshes: Flow<Unit>): Nothing =
    coroutineScope {
        val publish = rankedPublisher(context)
        launch { syncOverMessageStream(adapter, address, publish) }
        launch { syncOverVendorChannel(adapter, address, refreshes, publish) }
        launch { syncOverFastPair(context, adapter, refreshes, publish) }
        // Any route may give up (not offered, no permission); the sync still lives until the buds disconnect
        awaitCancellation()
    }

private typealias Publish = suspend (BatteryReading, ReadSource) -> Unit

private fun rankedPublisher(context: Context): Publish {
    val best = AtomicReference<ReadSource?>(null)
    val lock = Mutex()
    return { reading, source ->
        lock.withLock {
            if (shouldAccept(best.get(), source)) {
                if (best.get() != source) Log.d(TAG, "Live values now come from $source")
                best.set(source)
                publishReading(context, reading, source)
            }
        }
    }
}

private suspend fun syncOverMessageStream(adapter: BluetoothAdapter, address: String, publish: Publish) {
    var refusals = 0
    var failures = 0
    while (refusals < MESSAGE_STREAM_MAX_REFUSALS) {
        delay(RECONNECT_WAITS_MS[failures.coerceAtMost(RECONNECT_WAITS_MS.lastIndex)])
        val socket = connectMessageStream(adapter, address)
        if (socket == null) refusals++
        val heard = socket != null && runMessageStream(socket) { publish(it, ReadSource.MESSAGE_STREAM) }
        failures = if (heard) 0 else failures + 1
    }
    Log.d(TAG, "Message Stream refused $MESSAGE_STREAM_MAX_REFUSALS times; other routes only")
}

private suspend fun syncOverVendorChannel(
    adapter: BluetoothAdapter,
    address: String,
    refreshes: Flow<Unit>,
    publish: Publish,
) {
    if (offersVendorChannel(adapter, address) == false) {
        Log.d(TAG, "Buds don't offer the vendor channel")
        return
    }
    val requests = merge(refreshes, ticker(PULL_INTERVAL_MS))
    var failures = 0
    while (true) {
        delay(RECONNECT_WAITS_MS[failures.coerceAtMost(RECONNECT_WAITS_MS.lastIndex)])
        val socket = connectToBuds(adapter, address)
        val heard = socket != null && runSessionWithTimeout(socket, requests) { publish(it, ReadSource.VENDOR_CHANNEL) }
        failures = if (heard) 0 else failures + 1
    }
}

private suspend fun syncOverFastPair(context: Context, adapter: BluetoothAdapter, refreshes: Flow<Unit>, publish: Publish) =
    coroutineScope {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No scan permission; skipping the Fast Pair broadcast")
            return@coroutineScope
        }
        // Adverts repeat many times a second; only a change (or a refresh request) is published
        val lastPublished = AtomicReference<BatteryReading?>(null)
        launch { refreshes.collect { lastPublished.set(null) } }
        fastPairBatteryFlow(adapter).collect { reading ->
            if (lastPublished.getAndSet(reading) != reading) publish(reading, ReadSource.FAST_PAIR_ADVERT)
        }
    }

private fun ticker(intervalMs: Long): Flow<Unit> = flow {
    while (true) {
        delay(intervalMs)
        emit(Unit)
    }
}
