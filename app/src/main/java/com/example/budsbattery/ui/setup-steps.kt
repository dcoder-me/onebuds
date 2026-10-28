package com.example.budsbattery.ui

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
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
import com.example.budsbattery.bt.PairedDevice
import com.example.budsbattery.service.startBatteryRead
import kotlinx.coroutines.launch

private val ROW_PADDING = 16.dp
private val ROW_GAP = 12.dp
private val BADGE_SIZE = 28.dp
private const val DONE_MARK = "✓"

/** The one-time setup steps in one card. */
@Composable
fun SetupStepsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        PermissionStep()
        HorizontalDivider()
        BudsStep()
        HorizontalDivider()
        TileStep()
        HorizontalDivider()
        PinStep()
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
    // Re-checked on every return, so a permission granted in Settings shows as done
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
    var buds by remember { mutableStateOf<PairedDevice?>(null) }
    var linked by remember { mutableStateOf(false) }
    // Finding the buds also starts the sync, so opening the app is enough to get live values
    val refresh: suspend () -> Unit = {
        val found = detectBuds(context)
        buds = found
        linked = found?.let { isLinked(context, it.address) } ?: false
        if (found != null) startBatteryRead(context)
    }
    LifecycleResumeEffect(Unit) {
        val job = scope.launch { refresh() }
        onPauseOrDispose { job.cancel() }
    }
    val detect: () -> Unit = {
        scope.launch {
            refresh()
            if (buds == null) showToast(context, R.string.connect_buds_first)
        }
    }

    val onLinked: (AssociationInfo) -> Unit = { info ->
        scope.launch {
            saveLink(context, info)
            linked = true
        }
    }
    val linkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
            ?.let(onLinked)
    }
    val current = buds
    StepRow(
        number = 2,
        title = stringResource(R.string.step_buds_title),
        body = budsStepBody(current, linked),
        done = current != null && linked,
        actionLabel = stringResource(if (current == null) R.string.action_detect else R.string.action_link),
        onAction = {
            if (current == null) {
                detect()
            } else {
                linkBuds(context, current.address, { sender -> linkLauncher.launch(IntentSenderRequest.Builder(sender).build()) }, onLinked)
            }
        },
    )
}

@Composable
private fun budsStepBody(buds: PairedDevice?, linked: Boolean): String = when {
    buds == null -> stringResource(R.string.step_buds_body)
    linked -> stringResource(R.string.step_buds_linked, buds.name)
    else -> stringResource(R.string.step_buds_found, buds.name)
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
private fun PinStep() {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(canPinToTop(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = canPinToTop(context)
        onPauseOrDispose { }
    }
    StepRow(
        number = 4,
        title = stringResource(R.string.step_pin_title),
        body = stringResource(if (allowed) R.string.step_pin_done else R.string.step_pin_body),
        done = allowed,
        actionLabel = stringResource(R.string.action_allow),
        onAction = { openPinSettings(context) },
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
