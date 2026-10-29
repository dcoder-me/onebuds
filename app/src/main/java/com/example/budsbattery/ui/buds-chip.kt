package com.example.budsbattery.ui

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.budsbattery.R
import com.example.budsbattery.bt.PairedDevice
import com.example.budsbattery.service.startBatteryRead
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CHIP_ICON_SIZE = 18.dp
private const val DETECT_ATTEMPTS = 3
private const val DETECT_WAIT_MS = 2_000L

/**
 * The selected buds, top right. Detects and links by itself when the screen opens; tapping is the manual fallback.
 *
 * @param modifier - layout modifier
 */
@Composable
fun BudsChip(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var buds by remember { mutableStateOf<PairedDevice?>(null) }
    var linked by remember { mutableStateOf(false) }
    // The link dialog opens by itself once per screen; after a "Don't allow" only a tap brings it back
    var askedToLink by remember { mutableStateOf(false) }
    // Finding the buds also starts the sync, so opening the app is enough to get live values
    val refresh: suspend () -> Unit = {
        val found = detectBuds(context)
        buds = found
        linked = found?.let { isLinked(context, it.address) } ?: false
        if (found != null) startBatteryRead(context)
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
    val link: (PairedDevice) -> Unit = { found ->
        linkBuds(context, found.address, { sender -> linkLauncher.launch(IntentSenderRequest.Builder(sender).build()) }, onLinked)
    }
    val detectAndLink: suspend () -> Unit = {
        // The audio profiles come up a moment after the Bluetooth link, so a first miss is retried
        for (attempt in 1..DETECT_ATTEMPTS) {
            refresh()
            if (buds != null || attempt == DETECT_ATTEMPTS) break
            delay(DETECT_WAIT_MS)
        }
        // Android's start-from-background rule lets the sync start on connect only for linked buds
        val found = buds
        if (found != null && !linked && !askedToLink) {
            askedToLink = true
            link(found)
        }
    }
    var detecting by remember { mutableStateOf<Job?>(null) }
    // One detection at a time: a connect sends several profile events, and only the newest answer matters
    val redetect: () -> Unit = {
        detecting?.cancel()
        detecting = scope.launch { detectAndLink() }
    }
    LifecycleResumeEffect(Unit) {
        redetect()
        onPauseOrDispose { detecting?.cancel() }
    }
    BudsConnectionEffect(onChange = redetect)
    val current = buds
    AssistChip(
        modifier = modifier,
        onClick = {
            when {
                current == null -> scope.launch {
                    refresh()
                    if (buds == null) showToast(context, R.string.connect_buds_first)
                }
                !linked -> link(current)
                else -> scope.launch { refresh() }
            }
        },
        label = { Text(chipLabel(current, linked)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_buds), contentDescription = null, modifier = Modifier.size(CHIP_ICON_SIZE))
        },
    )
}

@Composable
private fun chipLabel(buds: PairedDevice?, linked: Boolean): String = when {
    buds == null -> stringResource(R.string.chip_detect)
    linked -> stringResource(R.string.chip_linked, buds.name)
    else -> stringResource(R.string.chip_link, buds.name)
}
