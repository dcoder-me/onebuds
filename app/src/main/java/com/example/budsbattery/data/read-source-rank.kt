package com.example.budsbattery.data

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
