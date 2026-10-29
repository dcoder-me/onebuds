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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "FastPairScan"
private val FAST_PAIR_SERVICE: ParcelUuid = ParcelUuid.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")

// === NOTE: === BLE adverts use random addresses, so they can't be matched to the buds' MAC; a weak
// signal floor keeps a neighbour's buds across the room from being read instead of ours
private const val MIN_RSSI = -70

/**
 * Listens to the Fast Pair battery broadcast for as long as it is collected, at low power.
 *
 * @description A filtered scan keeps running with the screen off, so this suits a foreground service.
 * @param adapter - Bluetooth adapter of the phone
 * @returns every nearby battery reading, as it arrives; empty when BLE is unavailable
 */
fun fastPairBatteryFlow(adapter: BluetoothAdapter): Flow<BatteryReading> {
    val scanner = adapter.bluetoothLeScanner ?: return emptyFlow()
    return fastPairAdverts(scanner, ScanSettings.SCAN_MODE_LOW_POWER)
        .filter { result -> result.rssi >= MIN_RSSI }
        .mapNotNull(::batteryOf)
}

private fun batteryOf(result: ScanResult): BatteryReading? =
    result.scanRecord?.getServiceData(FAST_PAIR_SERVICE)?.let(::parseFastPairBattery)

private fun fastPairAdverts(scanner: BluetoothLeScanner, scanMode: Int) = callbackFlow {
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
    val settings = ScanSettings.Builder().setScanMode(scanMode).build()
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
