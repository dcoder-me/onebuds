package com.example.budsbattery.data

// One bud or the case. Charging is the flag the buds send with the level.
data class BatteryLevel(val percent: Int, val charging: Boolean)

// Null means unknown: the part is out of the case, dead, or sent an invalid value.
data class BatteryReading(
    val left: BatteryLevel?,
    val right: BatteryLevel?,
    val case: BatteryLevel?,
)

data class BatterySnapshot(val reading: BatteryReading, val savedAtMillis: Long)

// Last saved values plus whether the buds are connected right now.
data class BatteryState(val snapshot: BatterySnapshot?, val connected: Boolean)
