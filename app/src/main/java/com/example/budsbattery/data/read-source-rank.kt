package com.example.budsbattery.data

/**
 * Tells whether a reading should be shown, given the routes that are open and answering right now.
 *
 * @description One route feeds values at a time: two routes that both answered used to take turns, so values
 * flipped between their slightly different reports. A route leaves the set the moment it closes, so the next one
 * takes over at once instead of values freezing.
 * @param liveRoutes - routes whose link is open and that have sent a reading on it
 * @param incoming - route of the new reading
 * @returns true when no live route ranks above the incoming one
 */
fun shouldAccept(liveRoutes: Set<ReadSource>, incoming: ReadSource): Boolean =
    liveRoutes.none { it.rank > incoming.rank }

/**
 * Tells whether a route should keep its link open, given the routes that are live right now.
 *
 * @description A route a better one outranks only adds radio traffic: its values would be ignored anyway, and
 * reconnecting it competes with the live link. It opens again the moment every better route closes.
 * @param liveRoutes - routes whose link is open and that have sent a reading on it
 * @param route - route asking whether to run
 * @returns true when no live route ranks above it
 */
fun routeNeeded(liveRoutes: Set<ReadSource>, route: ReadSource): Boolean =
    liveRoutes.none { it.rank > route.rank }
