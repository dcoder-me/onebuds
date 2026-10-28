package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import com.example.budsbattery.data.BatteryReading
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

/** How long to listen for the Fast Pair broadcast during one read. */
const val FAST_PAIR_SCAN_MS = 8_000L

/** Which route gave the values. */
enum class ReadSource { VENDOR_CHANNEL, FAST_PAIR }

/**
 * Outcome of one read across both routes. Route fields are null when that route was cut short by a win.
 *
 * @property vendorOffered - buds list the vendor channel in their service records; null if unknown
 */
data class ReadReport(
    val reading: BatteryReading?,
    val source: ReadSource?,
    val vendorOffered: Boolean?,
    val vendorConnected: Boolean?,
    val sawFastPair: Boolean?,
)

/**
 * Reads the buds over the vendor channel and the Fast Pair broadcast at once; the first answer wins.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the connected buds
 * @returns the reading if any route answered, plus what each route saw, for the failure message
 */
suspend fun readBatteryAnyRoute(adapter: BluetoothAdapter, address: String): ReadReport = coroutineScope {
    val vendorOffered = offersVendorChannel(adapter, address)
    val vendor = async { readBatteryOnce(adapter, address) }
    val fastPair = async { scanFastPairBattery(adapter, FAST_PAIR_SCAN_MS) }

    val firstWin = select<ReadReport?> {
        vendor.onAwait { result ->
            result.reading?.let { ReadReport(it, ReadSource.VENDOR_CHANNEL, vendorOffered, true, null) }
        }
        fastPair.onAwait { result ->
            result.reading?.let { ReadReport(it, ReadSource.FAST_PAIR, vendorOffered, null, true) }
        }
    }
    if (firstWin != null) {
        vendor.cancel()
        fastPair.cancel()
        return@coroutineScope firstWin
    }

    // The first route to finish had nothing; the other may still answer
    val vendorResult = vendor.await()
    val fastPairResult = fastPair.await()
    val source = when {
        vendorResult.reading != null -> ReadSource.VENDOR_CHANNEL
        fastPairResult.reading != null -> ReadSource.FAST_PAIR
        else -> null
    }
    ReadReport(
        reading = vendorResult.reading ?: fastPairResult.reading,
        source = source,
        vendorOffered = vendorOffered,
        vendorConnected = vendorResult.connected,
        sawFastPair = fastPairResult.sawFastPair,
    )
}
