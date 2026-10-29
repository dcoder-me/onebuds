package com.example.budsbattery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow

private val CARD_PADDING = 20.dp

/** Live battery values and where they come from. */
@Composable
fun BatteryCard() {
    val context = LocalContext.current
    val state by remember { batteryStateFlow(context.applicationContext) }
        .collectAsState(initial = BatteryState(snapshot = null, connected = false))
    val view = budsViewOf(state, System.currentTimeMillis())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BudsPanel(view)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(view.status, style = MaterialTheme.typography.bodyMedium)
                view.sourceNote?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
