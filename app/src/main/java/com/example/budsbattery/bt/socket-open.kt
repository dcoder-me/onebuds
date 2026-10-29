package com.example.budsbattery.bt

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a blocking Bluetooth connect off the main thread, and closes the socket if the caller was cancelled meanwhile.
 *
 * @description A Bluetooth connect can't be interrupted. When the buds left mid-connect, the connect still landed
 * after the sync was cancelled, and the dropped result kept the channel open. The buds allow one client per channel,
 * so every later connect, ours and Realme Link's, was refused until the app process died.
 * @param open - blocking connect; returns the open socket or null
 * @param close - closes a socket nobody will use
 * @returns the open socket, or null if the connect failed
 * @throws CancellationException when cancelled; the socket is closed first
 */
suspend fun <T : Any> openUnlessCancelled(open: () -> T?, close: (T) -> Unit): T? {
    // === NOTE: === A cancelled caller drops withContext's result on the way back, so the socket is kept here too
    val opened = AtomicReference<T?>(null)
    try {
        return withContext(Dispatchers.IO) { open().also { opened.set(it) } }
    } catch (cancelled: CancellationException) {
        opened.getAndSet(null)?.let(close)
        throw cancelled
    }
}
