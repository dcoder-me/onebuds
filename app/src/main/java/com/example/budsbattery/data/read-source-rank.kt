package com.example.budsbattery.data

/**
 * Tells whether a reading should be shown, given the best route heard during this connection.
 *
 * @description The broadcast lags and made values flip, so it is dropped once the buds answer directly.
 * Both direct routes stay accepted: locking onto one froze values whenever that one went quiet.
 * @param bestSoFar - highest-ranked route that answered during this connection, or null
 * @param incoming - route of the new reading
 * @returns true when the new reading comes from the buds directly, or nothing better has answered yet
 */
fun shouldAccept(bestSoFar: ReadSource?, incoming: ReadSource): Boolean =
    bestSoFar == null || incoming.direct || !bestSoFar.direct
