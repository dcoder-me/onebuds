package com.example.budsbattery.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R

private val SCREEN_PADDING = 16.dp
private val SECTION_SPACING = 12.dp
private val HEADER_TOP_PADDING = 8.dp

/** The app screen: buds on top right, live battery, then on/off settings. Also opens from the tile's long-press. */
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15+ always draws behind the system bars, so the screen pads itself instead
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BudsTheme { SetupScreen() }
        }
    }
}

@Composable
private fun SetupScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(SCREEN_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HEADER_TOP_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                BudsChip()
            }
            PermissionCard()
            BatteryCard()
            SettingsCard()
        }
    }
}
