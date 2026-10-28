package com.example.budsbattery.data

// One bud or the case. Charging is the flag the buds send with the level.
data class BatteryLevel(val percent: Int, val charging: Boolean)

// Null means unknown: the part is out of reach, dead, or sent an invalid value.
data class BatteryReading(
    val left: BatteryLevel?,
    val right: BatteryLevel?,
    val case: BatteryLevel?,
)

// Which route gave the values. A higher rank is closer to the buds and wins while it keeps answering.
enum class ReadSource(val rank: Int) {
    FAST_PAIR_ADVERT(0),
    VENDOR_CHANNEL(1),
    MESSAGE_STREAM(2),
}

// The case level is only known while a bud sits in it, so it keeps its own "seen at" time.
data class BatterySnapshot(
    val reading: BatteryReading,
    val savedAtMillis: Long,
    val caseSeenAtMillis: Long? = null,
    val source: ReadSource? = null,
)

// Last saved values, whether the buds are connected, and when this connection began.
data class BatteryState(
    val snapshot: BatterySnapshot?,
    val connected: Boolean,
    val connectedAtMillis: Long? = null,
)
