package com.example.budsbattery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow
import kotlinx.coroutines.launch

private val CARD_PADDING = 20.dp
private val SPINNER_SIZE = 18.dp
private val SPINNER_STROKE = 2.dp

/** Live battery values with a button to read them now. */
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
            ReadNowButton()
        }
    }
}

@Composable
private fun ReadNowButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    Button(
        enabled = !reading,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            scope.launch {
                reading = true
                failure = null
                try {
                    failure = readNow(context)
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
    // Says which route failed and how, so the fix is clear without a laptop
    failure?.let { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
        )
    }
}
