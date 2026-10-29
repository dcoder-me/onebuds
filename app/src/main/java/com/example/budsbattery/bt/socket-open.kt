package com.example.budsbattery.bt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a blocking Bluetooth connect off the main thread.
 *
 * @param open - blocking connect; returns the open socket or null
 * @param close - closes a socket nobody will use
 * @returns the open socket, or null if the connect failed
 */
suspend fun <T : Any> openUnlessCancelled(open: () -> T?, close: (T) -> Unit): T? =
    withContext(Dispatchers.IO) { open() }
