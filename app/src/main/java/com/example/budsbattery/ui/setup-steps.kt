package com.example.budsbattery.ui

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.budsbattery.bt.PairedDevice
import com.example.budsbattery.data.readSelectedAddress
import com.example.budsbattery.data.saveSelectedAddress
import com.example.budsbattery.service.hasBluetoothPermission
import kotlinx.coroutines.launch

private val ROW_PADDING = 16.dp
private val ROW_GAP = 12.dp
private val BADGE_SIZE = 28.dp
private const val DONE_MARK = "✓"

/** The three one-time setup steps in one card. */
@Composable
fun SetupStepsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        PermissionStep()
        HorizontalDivider()
        BudsStep()
        HorizontalDivider()
        TileStep()
    }
}

/** Switch that shows or hides the launcher icon. */
@Composable
fun IconCard() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(isLauncherIconVisible(context)) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        ) {
            StepText(stringResource(R.string.show_app_icon), stringResource(R.string.show_app_icon_body), Modifier.weight(1f))
            Switch(
                checked = visible,
                onCheckedChange = { checked ->
                    setLauncherIconVisible(context, checked)
                    visible = checked
                },
            )
        }
    }
}

@Composable
private fun PermissionStep() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasSetupPermissions(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasSetupPermissions(context)
        // Android hides the dialog after two denials, so the only way left is Settings
        if (!granted) {
            showToast(context, R.string.allow_in_settings)
            openAppSettings(context)
        }
    }
    StepRow(
        number = 1,
        title = stringResource(R.string.step_permissions_title),
        body = stringResource(R.string.step_permissions_body),
        done = granted,
        actionLabel = stringResource(R.string.action_allow),
        onAction = { launcher.launch(SETUP_PERMISSIONS) },
    )
}

@Composable
private fun BudsStep() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf<String?>(null) }
    var devices by remember { mutableStateOf<List<PairedDevice>?>(null) }
    LaunchedEffect(Unit) { address = readSelectedAddress(context) }

    val onLinked: (AssociationInfo) -> Unit = { info -> scope.launch { savePickedBuds(context, info) } }
    val linkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
            ?.let(onLinked)
    }
    val onPick: (PairedDevice) -> Unit = { device ->
        devices = null
        scope.launch {
            // Saved before the link step, so Read now works even if Android refuses the link
            saveSelectedAddress(context, device.address)
            address = device.address
            linkBuds(context, device.address, { sender -> linkLauncher.launch(IntentSenderRequest.Builder(sender).build()) }, onLinked)
        }
    }

    devices?.let { BudsPickerDialog(it, onPick, onDismiss = { devices = null }) }
    StepRow(
        number = 2,
        title = stringResource(R.string.step_buds_title),
        body = address?.let { stringResource(R.string.step_buds_done, budsLabelFor(context, it)) }
            ?: stringResource(R.string.step_buds_body),
        done = address != null,
        actionLabel = stringResource(if (address == null) R.string.action_choose else R.string.action_change),
        onAction = { devices = pairedDevicesOrToast(context) },
        redoable = true,
    )
}

// Returns null (no dialog) and explains why when there is nothing to pick from
private fun pairedDevicesOrToast(context: Context): List<PairedDevice>? {
    if (!hasBluetoothPermission(context)) {
        showToast(context, R.string.allow_permissions_first)
        return null
    }
    val devices = pairedDevicesFor(context)
    if (devices.isEmpty()) showToast(context, R.string.no_paired_devices)
    return devices.ifEmpty { null }
}

@Composable
private fun TileStep() {
    val context = LocalContext.current
    StepRow(
        number = 3,
        title = stringResource(R.string.step_tile_title),
        body = stringResource(R.string.step_tile_body),
        done = false,
        actionLabel = stringResource(R.string.action_add),
        onAction = { requestTile(context) },
    )
}

@Composable
private fun StepRow(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    redoable: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        StepBadge(number, done)
        StepText(title, body, Modifier.weight(1f))
        if (!done || redoable) {
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StepBadge(number: Int, done: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BADGE_SIZE)
            .background(if (done) colors.primary else colors.surfaceVariant, CircleShape),
    ) {
        Text(
            text = if (done) DONE_MARK else number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (done) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepText(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
