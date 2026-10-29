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
    ;

    // Sent by the buds over a live link, not the lagging broadcast
    val direct: Boolean
        get() = this != FAST_PAIR_ADVERT
}

// The latest reading as sent. The case is in it only while a bud sits in the case, like AirPods.
data class BatterySnapshot(
    val reading: BatteryReading,
    val savedAtMillis: Long,
    val source: ReadSource? = null,
)

// Last saved values, whether the buds are connected, when this connection began, and whether the
// buds report a bud in the case (null until they say so during this connection).
data class BatteryState(
    val snapshot: BatterySnapshot?,
    val connected: Boolean,
    val connectedAtMillis: Long? = null,
    val budInCase: Boolean? = null,
)
