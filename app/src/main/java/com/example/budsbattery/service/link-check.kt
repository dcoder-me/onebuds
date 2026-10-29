package com.example.budsbattery.service

import kotlinx.coroutines.delay

/**
 * Looks again until the device is gone or every wait is used up.
 *
 * @description Android drops the audio profiles a moment after the Bluetooth link, so a single look right at a
 * link drop can still see the buds. That kept the sync running, and the screen on "connected", after they left.
 * @param waitsMs - pause before each look; the first is usually 0
 * @param check - returns the device while it is still there, or null once it is gone
 * @returns null as soon as a look finds nothing, or the device when it stays through every look
 */
suspend fun <T : Any> awaitGone(waitsMs: List<Long>, check: suspend () -> T?): T? {
    var last: T? = null
    for (wait in waitsMs) {
        delay(wait)
        last = check() ?: return null
    }
    return last
}
