package com.example.budsbattery.bt

private val BUDS_NAME_HINTS = listOf("realme", "buds")

/** A paired Bluetooth device, by name and MAC address. */
data class PairedDevice(val name: String, val address: String)

/**
 * Tells whether a device name looks like earbuds, so buds win over a car kit or speaker.
 *
 * @param name - device name from Bluetooth
 * @returns true when the name contains "realme" or "buds"
 */
fun looksLikeBuds(name: String): Boolean = BUDS_NAME_HINTS.any { hint -> name.contains(hint, ignoreCase = true) }
