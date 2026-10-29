package com.example.budsbattery.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.budsbattery.R
import com.example.budsbattery.data.liveUpdateFlow
import com.example.budsbattery.data.saveLiveUpdate
import com.example.budsbattery.service.refreshSurfaces
import kotlinx.coroutines.launch

private val ROW_PADDING = 16.dp
private val ROW_GAP = 12.dp

/** Asks for the permissions the app needs. Shown only while one is missing. */
@Composable
fun PermissionCard() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasSetupPermissions(context)) }
    // Re-checked on every return, so a permission granted in Settings hides the card
    LifecycleResumeEffect(Unit) {
        granted = hasSetupPermissions(context)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasSetupPermissions(context)
        // Android hides the dialog after two denials, so the only way left is Settings
        if (!granted) {
            showToast(context, R.string.allow_in_settings)
            openAppSettings(context)
        }
    }
    if (granted) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        SettingRow(stringResource(R.string.permissions_title), stringResource(R.string.permissions_body)) {
            FilledTonalButton(onClick = { launcher.launch(SETUP_PERMISSIONS) }) { Text(stringResource(R.string.action_allow)) }
        }
    }
}

/** On/off switches: Live Update and the launcher icon. */
@Composable
fun SettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        LiveUpdateRow()
        HorizontalDivider()
        IconRow()
    }
}

@Composable
private fun LiveUpdateRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by remember { liveUpdateFlow(context.applicationContext) }.collectAsState(initial = true)
    var allowed by remember { mutableStateOf(canPinToTop(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = canPinToTop(context)
        onPauseOrDispose { }
    }
    // On only when both the app wants it and Android allows it, so the switch never claims what the phone refuses
    val on = enabled && allowed
    val body = when {
        on -> R.string.live_update_on
        enabled -> R.string.live_update_blocked
        else -> R.string.live_update_off
    }
    SettingRow(stringResource(R.string.live_update_title), stringResource(body)) {
        Switch(
            checked = on,
            onCheckedChange = { checked ->
                scope.launch {
                    saveLiveUpdate(context, checked)
                    refreshSurfaces(context.applicationContext)
                }
                if (checked && !allowed) openPinSettings(context)
            },
        )
    }
}

@Composable
private fun IconRow() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(isLauncherIconVisible(context)) }
    SettingRow(stringResource(R.string.show_app_icon), stringResource(R.string.show_app_icon_body)) {
        Switch(
            checked = visible,
            onCheckedChange = { checked ->
                setLauncherIconVisible(context, checked)
                visible = checked
            },
        )
    }
}

@Composable
private fun SettingRow(title: String, body: String, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action()
    }
}
