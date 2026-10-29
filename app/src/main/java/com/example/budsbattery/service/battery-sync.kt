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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "BatterySync"

// The buds push changes on their own; this pull is a safety net in case a push is missed. A request is a
// few bytes on a link that is already open, so a short interval costs next to nothing
private const val PULL_INTERVAL_MS = 20_000L

// Wait before each reconnect: none at first. The buds are known to be connected while this runs,
// so a dropped channel is retried within seconds; the old 5-minute cap froze values for minutes
private val RECONNECT_WAITS_MS = listOf(0L, 1_000L, 2_000L, 5_000L, 15_000L)

// A refused Message Stream usually means Google services hold it; stop trying after this many
private const val MESSAGE_STREAM_MAX_REFUSALS = 3

/**
 * Keeps battery values fresh until cancelled. Three routes run side by side; only the best one that is open
 * right now feeds values, so routes never take turns and a closed route hands over at once.
 *
 * @param context - app context
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the connected buds
 * @param refreshes - each emission asks the routes for fresh values now
 */
suspend fun runBatterySync(context: Context, adapter: BluetoothAdapter, address: String, refreshes: Flow<Unit>): Nothing =
    coroutineScope {
        val routes = LiveRoutes(context)
        launch { syncOverMessageStream(adapter, address, routes) }
        launch { syncOverVendorChannel(context, adapter, address, refreshes, routes) }
        launch { syncOverFastPair(context, adapter, refreshes, routes) }
        // Any route may give up (not offered, no permission); the sync still lives until the buds disconnect
        awaitCancellation()
    }

// Routes whose link is open and has answered. Saving is all a reading does here: the service draws the surfaces
// from the saved state on its own, so a slow widget never holds up the next frame from the buds
private class LiveRoutes(private val context: Context) {
    val open = MutableStateFlow<Set<ReadSource>>(emptySet())

    suspend fun publish(reading: BatteryReading, source: ReadSource) {
        if (source.direct) open.update { it + source }
        val accepted = shouldAccept(open.value, source)
        // One line per reading, so a wrong value (such as a low case) can be traced to the route that sent it
        Log.d(TAG, "$source L=${reading.left} R=${reading.right} case=${reading.case} ${if (accepted) "shown" else "ignored"}")
        if (accepted) publishReading(context, reading, source)
    }

    fun closed(source: ReadSource) {
        open.update { it - source }
        Log.d(TAG, "$source closed; open routes: ${open.value}")
    }
}

private suspend fun syncOverMessageStream(adapter: BluetoothAdapter, address: String, routes: LiveRoutes) {
    var refusals = 0
    var failures = 0
    while (refusals < MESSAGE_STREAM_MAX_REFUSALS) {
        delay(RECONNECT_WAITS_MS[failures.coerceAtMost(RECONNECT_WAITS_MS.lastIndex)])
        val socket = connectMessageStream(adapter, address)
        if (socket == null) refusals++
        val heard = socket != null && runMessageStream(socket) { routes.publish(it, ReadSource.MESSAGE_STREAM) }
        routes.closed(ReadSource.MESSAGE_STREAM)
        failures = if (heard) 0 else failures + 1
    }
    Log.d(TAG, "Message Stream refused $MESSAGE_STREAM_MAX_REFUSALS times; other routes only")
}

private suspend fun syncOverVendorChannel(
    context: Context,
    adapter: BluetoothAdapter,
    address: String,
    refreshes: Flow<Unit>,
    routes: LiveRoutes,
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
        val heard = socket != null && runSessionWithTimeout(
            socket,
            requests,
            onPlacement = { inCase ->
                Log.d(TAG, "Bud in case: $inCase")
                publishPlacement(context, inCase)
            },
        ) { routes.publish(it, ReadSource.VENDOR_CHANNEL) }
        routes.closed(ReadSource.VENDOR_CHANNEL)
        failures = if (heard) 0 else failures + 1
    }
}

// === NOTE: === BLE scanning shares the radio with the open Bluetooth link and slows it, so the scan runs only
// while no direct route is open, and starts again by itself if they all close
private suspend fun syncOverFastPair(context: Context, adapter: BluetoothAdapter, refreshes: Flow<Unit>, routes: LiveRoutes) {
    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "No scan permission; skipping the Fast Pair broadcast")
        return
    }
    routes.open.map { open -> open.any { it.direct } }.distinctUntilChanged().collectLatest { directOpen ->
        if (!directOpen) scanFastPair(adapter, refreshes, routes)
    }
}

private suspend fun scanFastPair(adapter: BluetoothAdapter, refreshes: Flow<Unit>, routes: LiveRoutes) = coroutineScope {
    // Adverts repeat many times a second; only a change (or a refresh request) is published
    val lastPublished = AtomicReference<BatteryReading?>(null)
    launch { refreshes.collect { lastPublished.set(null) } }
    fastPairBatteryFlow(adapter).collect { reading ->
        if (lastPublished.getAndSet(reading) != reading) routes.publish(reading, ReadSource.FAST_PAIR_ADVERT)
    }
}

private fun ticker(intervalMs: Long): Flow<Unit> = flow {
    while (true) {
        delay(intervalMs)
        emit(Unit)
    }
}
