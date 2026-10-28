package com.example.budsbattery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow
import kotlinx.coroutines.launch

private val CARD_PADDING = 20.dp
private val GAUGE_SIZE = 84.dp
private val GAUGE_STROKE = 8.dp
private val SPINNER_SIZE = 18.dp
private val SPINNER_STROKE = 2.dp
private const val FULL_PERCENT = 100f

/** Live battery values with a button to read them now. */
@Composable
fun BatteryCard() {
    val context = LocalContext.current
    val state by remember { batteryStateFlow(context.applicationContext) }
        .collectAsState(initial = BatteryState(snapshot = null, connected = false))
    val reading = state.snapshot?.reading

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BatteryGauge(stringResource(R.string.part_left), reading?.left)
                BatteryGauge(stringResource(R.string.part_right), reading?.right)
                BatteryGauge(stringResource(R.string.part_case), reading?.case)
            }
            Text(
                text = formatStatusLine(state, System.currentTimeMillis()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            ReadNowButton()
        }
    }
}

@Composable
private fun BatteryGauge(label: String, level: BatteryLevel?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(GAUGE_SIZE)) {
            CircularProgressIndicator(
                progress = { (level?.percent ?: 0) / FULL_PERCENT },
                modifier = Modifier.size(GAUGE_SIZE),
                strokeWidth = GAUGE_STROKE,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Text(
                text = level?.let { "${it.percent}%" } ?: stringResource(R.string.unknown_level),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        if (level?.charging == true) {
            Text(
                text = "⚡ ${stringResource(R.string.charging)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ReadNowButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }

    Button(
        enabled = !reading,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            scope.launch {
                reading = true
                try {
                    readNow(context)
                } finally {
                    reading = false
                }
            }
        },
    ) {
        if (reading) {
            CircularProgressIndicator(modifier = Modifier.size(SPINNER_SIZE), strokeWidth = SPINNER_STROKE)
            Text(stringResource(R.string.reading), modifier = Modifier.padding(start = 8.dp))
        } else {
            Text(stringResource(R.string.read_now))
        }
    }
}
