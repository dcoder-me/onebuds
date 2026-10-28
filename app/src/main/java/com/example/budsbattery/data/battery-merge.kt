package com.example.budsbattery.data

/**
 * Folds a new reading into the saved one.
 *
 * @description Buds are taken as sent: a missing bud is out of reach, so one bud in the ear shows as one.
 * The case is kept from before when the reading lacks it, since buds only learn the case level
 * while sitting in it; its own time says how old it is.
 * @param previous - the saved snapshot, or null before the first reading
 * @param incoming - values just read
 * @param source - route that gave the values
 * @param nowMillis - current time in epoch milliseconds
 * @returns the snapshot to save
 */
fun mergeSnapshot(previous: BatterySnapshot?, incoming: BatteryReading, source: ReadSource, nowMillis: Long): BatterySnapshot {
    val caseIsNew = incoming.case != null
    return BatterySnapshot(
        reading = incoming.copy(case = incoming.case ?: previous?.reading?.case),
        savedAtMillis = nowMillis,
        caseSeenAtMillis = if (caseIsNew) nowMillis else previous?.caseSeenAtMillis,
        source = source,
    )
}

/**
 * Tells whether a reading should be shown, given the best route heard during this connection.
 *
 * @description Two routes overwriting each other made values flip; the route closest to the buds wins.
 * @param bestSoFar - highest-ranked route that answered during this connection, or null
 * @param incoming - route of the new reading
 * @returns true when the new reading comes from the same or a better route
 */
fun shouldAccept(bestSoFar: ReadSource?, incoming: ReadSource): Boolean =
    bestSoFar == null || incoming.rank >= bestSoFar.rank
