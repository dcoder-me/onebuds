package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.protocol.parseFastPairBattery
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "FastPairScan"
private val FAST_PAIR_SERVICE: ParcelUuid = ParcelUuid.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")

// === NOTE: === BLE adverts use random addresses, so they can't be matched to the buds' MAC; a weak
// signal floor keeps a neighbour's buds across the room from being read instead of ours
private const val MIN_RSSI = -70

/** What a Fast Pair scan found. sawFastPair tells "no adverts at all" apart from "adverts without battery". */
data class FastPairScanResult(val reading: BatteryReading?, val sawFastPair: Boolean)

/**
 * Listens for the Fast Pair battery broadcast that drives the system popup.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param timeoutMs - how long to listen
 * @returns the first nearby battery reading, and whether any Fast Pair advert was seen
 */
suspend fun scanFastPairBattery(adapter: BluetoothAdapter, timeoutMs: Long): FastPairScanResult {
    val scanner = adapter.bluetoothLeScanner ?: return FastPairScanResult(null, sawFastPair = false)
    var sawFastPair = false
    val reading = withTimeoutOrNull(timeoutMs) {
        fastPairAdverts(scanner)
            .firstOrNull { result ->
                sawFastPair = true
                result.rssi >= MIN_RSSI && batteryOf(result) != null
            }
            ?.let(::batteryOf)
    }
    return FastPairScanResult(reading, sawFastPair)
}

private fun batteryOf(result: ScanResult): BatteryReading? =
    result.scanRecord?.getServiceData(FAST_PAIR_SERVICE)?.let(::parseFastPairBattery)

private fun fastPairAdverts(scanner: BluetoothLeScanner) = callbackFlow {
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            trySend(result)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Scan failed: $errorCode")
            close()
        }
    }
    val filter = ScanFilter.Builder().setServiceData(FAST_PAIR_SERVICE, ByteArray(0)).build()
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    try {
        scanner.startScan(listOf(filter), settings, callback)
    } catch (error: SecurityException) {
        Log.w(TAG, "Scan permission missing", error)
        close()
    }
    awaitClose {
        try {
            scanner.stopScan(callback)
        } catch (error: SecurityException) {
            Log.w(TAG, "Scan permission missing on stop", error)
        }
    }
}
