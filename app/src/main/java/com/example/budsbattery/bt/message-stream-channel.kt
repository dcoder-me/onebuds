package com.example.budsbattery.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.protocol.parseMessageStreamBattery
import com.example.budsbattery.protocol.splitMessages
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MessageStream"

// Fixed RFCOMM endpoint from the Fast Pair Message Stream spec
private val MESSAGE_STREAM_UUID: UUID = UUID.fromString("df21fe2c-2515-4fdb-8886-f12c4d67927c")
private const val READ_CHUNK_BYTES = 256
private const val BYTE_MASK = 0xFF

/**
 * Opens the Fast Pair Message Stream, Google's channel for live battery while the buds are connected.
 *
 * @param adapter - Bluetooth adapter of the phone
 * @param address - MAC address of the buds
 * @returns a connected socket, or null if the buds refused (for example, Google services already hold it)
 */
suspend fun connectMessageStream(adapter: BluetoothAdapter, address: String): BluetoothSocket? =
    openUnlessCancelled(open = { openMessageStream(adapter, address) }, close = { it.close() })

private fun openMessageStream(adapter: BluetoothAdapter, address: String): BluetoothSocket? {
    val socket = try {
        adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(MESSAGE_STREAM_UUID)
    } catch (error: SecurityException) {
        Log.w(TAG, "Bluetooth permission missing", error)
        return null
    } catch (error: IOException) {
        Log.w(TAG, "Couldn't create the socket", error)
        return null
    }
    return try {
        socket.connect()
        Log.d(TAG, "Connected")
        socket
    } catch (error: IOException) {
        Log.w(TAG, "Buds refused the Message Stream", error)
        socket.close()
        null
    }
}

/**
 * Reads messages until the buds close the stream; the buds send battery on open and on every change.
 *
 * @param socket - connected Message Stream; always closed when this returns
 * @param onReading - called for each battery reading
 * @returns true if at least one battery reading arrived
 */
suspend fun runMessageStream(socket: BluetoothSocket, onReading: suspend (BatteryReading) -> Unit): Boolean =
    coroutineScope {
        // Cancelling must close the socket, or the blocking read never returns
        val closeOnCancel = launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                socket.close()
            }
        }
        var heard = false
        try {
            withContext(Dispatchers.IO) {
                readMessages(socket) { reading ->
                    heard = true
                    onReading(reading)
                }
            }
        } catch (error: IOException) {
            Log.d(TAG, "Stream ended: ${error.message}")
        } finally {
            closeOnCancel.cancel()
            socket.close()
        }
        heard
    }

private suspend fun readMessages(socket: BluetoothSocket, onReading: suspend (BatteryReading) -> Unit) {
    val input = socket.inputStream
    val chunk = ByteArray(READ_CHUNK_BYTES)
    var pending = ByteArray(0)
    while (true) {
        val count = input.read(chunk)
        if (count < 0) throw IOException("Buds closed the stream")
        val split = splitMessages(pending + chunk.copyOf(count))
        pending = split.leftover
        for (message in split.messages) {
            // Raw bytes are logged so other events can be checked against the spec by hand
            Log.d(TAG, "Message: ${message.joinToString(" ") { "%02X".format(it.toInt() and BYTE_MASK) }}")
            parseMessageStreamBattery(message)?.let { onReading(it) }
        }
    }
}
