package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.protocol.BatteryFrameResult
import com.example.budsbattery.protocol.encodeBatteryRequest
import com.example.budsbattery.protocol.encodeBatterySubscription
import com.example.budsbattery.protocol.parseBatteryFrame
import com.example.budsbattery.protocol.splitFrames
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BudsChannel"

// === NOTE: === T300 control channel from Gadgetbridge first, then plain serial (SPP) for models that only expose that
private val BUDS_CHANNEL_UUIDS: List<UUID> = listOf(
    UUID.fromString("0000079a-d102-11e1-9b23-00025b00a5a5"),
    UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"),
)

private const val REPLY_TIMEOUT_MS = 3_000L
private const val READ_CHUNK_BYTES = 256
private const val BYTE_MASK = 0xFF
private const val SUBSCRIBE_SEQ = 0
private const val REQUEST_SEQ = 1
private const val SEQ_MASK = 0xFF

/**
 * Opens the control channel. Returns null instead of throwing, so callers can retry.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the buds
 * @returns a connected socket, or null if the buds refused or permission is missing
 */
suspend fun connectToBuds(adapter: BluetoothAdapter, address: String): BluetoothSocket? =
    // Connecting blocks for seconds; off the main thread or the screen freezes
    withContext(Dispatchers.IO) {
        BUDS_CHANNEL_UUIDS.firstNotNullOfOrNull { uuid -> tryOpenBudsSocket(adapter, address, uuid) }
    }

private fun tryOpenBudsSocket(adapter: BluetoothAdapter, address: String, uuid: UUID): BluetoothSocket? = try {
    openBudsSocket(adapter, address, uuid).also { Log.d(TAG, "Connected on $uuid") }
} catch (error: IOException) {
    Log.w(TAG, "Buds refused $uuid", error)
    null
} catch (error: SecurityException) {
    Log.w(TAG, "Bluetooth permission missing", error)
    null
} catch (error: IllegalArgumentException) {
    Log.w(TAG, "Bad address $address", error)
    null
}

/**
 * Subscribes to battery pushes and asks once, then asks again on each signal, reading every frame until the channel closes.
 *
 * @param socket - connected control channel
 * @param requests - each emission sends one more battery request (pull refresh while connected)
 * @param onReading - called for each battery reading
 * @throws IOException when the buds drop the channel
 */
suspend fun runBudsSession(
    socket: BluetoothSocket,
    requests: Flow<Unit> = emptyFlow(),
    onReading: suspend (BatteryReading) -> Unit,
) = coroutineScope {
    // Cancelling the session must close the socket, or the blocking read never returns
    val closeOnCancel = launch(Dispatchers.IO) {
        try {
            awaitCancellation()
        } finally {
            socket.close()
        }
    }
    // All writes stay on this one coroutine, so frames never interleave on the stream
    val writer = launch(Dispatchers.IO) {
        val output = socket.outputStream
        output.write(encodeBatterySubscription(SUBSCRIBE_SEQ))
        output.write(encodeBatteryRequest(REQUEST_SEQ))
        output.flush()
        var seq = REQUEST_SEQ
        requests.collect {
            seq = (seq + 1) and SEQ_MASK
            output.write(encodeBatteryRequest(seq))
            output.flush()
        }
    }
    try {
        withContext(Dispatchers.IO) { readFrames(socket.inputStream, onReading) }
    } finally {
        writer.cancel()
        closeOnCancel.cancel()
    }
}

/**
 * Runs one session. Closes the channel if the buds stay silent for too long.
 *
 * @param socket - connected control channel; always closed when this returns
 * @param requests - each emission sends one more battery request
 * @param onReading - called for each battery reading
 * @returns true if the buds sent at least one reading
 */
suspend fun runSessionWithTimeout(
    socket: BluetoothSocket,
    requests: Flow<Unit> = emptyFlow(),
    onReading: suspend (BatteryReading) -> Unit,
): Boolean =
    coroutineScope {
        val heard = AtomicBoolean(false)
        val watchdog = launch(Dispatchers.IO) {
            delay(REPLY_TIMEOUT_MS)
            if (!heard.get()) socket.close()
        }
        try {
            runBudsSession(socket, requests) { reading ->
                heard.set(true)
                onReading(reading)
            }
        } catch (error: IOException) {
            Log.d(TAG, "Session ended: ${error.message}")
        }
        watchdog.cancel()
        socket.close()
        heard.get()
    }

/** What one vendor-channel read found. connected tells "buds refused" apart from "buds stayed silent". */
data class VendorReadResult(val reading: BatteryReading?, val connected: Boolean)

/**
 * Reads the buds once over the vendor channel, for the "Read now" button in setup.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the buds
 * @returns the first reading if any, and whether the channel opened at all
 */
suspend fun readBatteryOnce(adapter: BluetoothAdapter, address: String): VendorReadResult {
    val socket = connectToBuds(adapter, address) ?: return VendorReadResult(null, connected = false)
    val reading = AtomicReference<BatteryReading?>(null)
    runSessionWithTimeout(socket) { value ->
        reading.set(value)
        socket.close()
    }
    return VendorReadResult(reading.get(), connected = true)
}

/**
 * Tells whether the buds list the vendor channel in their service records (SDP, cached by Android).
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the buds
 * @returns true or false, or null when Android has no cached record or permission is missing
 */
fun offersVendorChannel(adapter: BluetoothAdapter, address: String): Boolean? = try {
    adapter.getRemoteDevice(address).uuids?.any { it.uuid == BUDS_CHANNEL_UUIDS.first() }
} catch (error: SecurityException) {
    Log.w(TAG, "Bluetooth permission missing", error)
    null
} catch (error: IllegalArgumentException) {
    Log.w(TAG, "Bad address $address", error)
    null
}

private fun openBudsSocket(adapter: BluetoothAdapter, address: String, uuid: UUID): BluetoothSocket {
    val socket = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(uuid)
    try {
        socket.connect()
        return socket
    } catch (error: IOException) {
        socket.close()
        throw error
    }
}

private suspend fun readFrames(input: InputStream, onReading: suspend (BatteryReading) -> Unit) {
    val chunk = ByteArray(READ_CHUNK_BYTES)
    var pending = ByteArray(0)
    while (true) {
        val count = input.read(chunk)
        if (count < 0) throw IOException("Buds closed the channel")
        val split = splitFrames(pending + chunk.copyOf(count))
        pending = split.leftover
        for (frame in split.frames) handleFrame(frame, onReading)
    }
}

// Raw bytes are logged so a bad frame can be checked by hand against the protocol
private suspend fun handleFrame(frame: ByteArray, onReading: suspend (BatteryReading) -> Unit) {
    Log.d(TAG, "Frame: ${frame.toHex()}")
    when (val result = parseBatteryFrame(frame)) {
        is BatteryFrameResult.Reading -> onReading(result.reading)
        is BatteryFrameResult.Rejected -> Log.w(TAG, "Rejected frame (${result.reason}): ${frame.toHex()}")
        BatteryFrameResult.Ignored -> Unit
    }
}

private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and BYTE_MASK) }
