package com.example.budsbattery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow
import kotlinx.coroutines.delay

private const val CLOCK_TICK_MS = 30_000L

/**
 * Battery card for the Quick Settings panel: left, right and case as three boxes with rings.
 */
@Composable
fun BatteryBoxesCard() {
    val context = LocalContext.current
    val state by remember { batteryStateFlow(context.applicationContext) }
        .collectAsState(initial = BatteryState(snapshot = null, connected = false))
    // "read 2h ago" keeps counting while the card is open
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    val view = budsViewOf(state, now)
    PanelCardHeader(stringResource(R.string.app_subtitle), view.status)
    BudsBoxes(view)
}
