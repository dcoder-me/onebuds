package com.example.budsbattery.ui

import com.example.budsbattery.bt.ReadReport

private const val VENDOR_NOT_OFFERED = "Vendor channel: these buds don't offer it"
private const val VENDOR_REFUSED = "Vendor channel: buds refused the connection"
private const val VENDOR_SILENT = "Vendor channel: opened, but no battery reply"
private const val FAST_PAIR_NONE = "Fast Pair: no broadcast. Open the case next to the phone."
private const val FAST_PAIR_NO_BATTERY = "Fast Pair: broadcast seen, but without battery"

/**
 * Explains in plain words why a read found nothing, one line per route.
 *
 * @param report - outcome of a read where no route answered
 * @returns lines such as "Vendor channel: buds refused the connection"; empty when the read worked
 */
fun formatReadFailure(report: ReadReport): String {
    if (report.reading != null) return ""
    val vendor = when {
        report.vendorConnected == true -> VENDOR_SILENT
        report.vendorOffered == false -> VENDOR_NOT_OFFERED
        else -> VENDOR_REFUSED
    }
    val fastPair = if (report.sawFastPair == true) FAST_PAIR_NO_BATTERY else FAST_PAIR_NONE
    return "$vendor\n$fastPair"
}
