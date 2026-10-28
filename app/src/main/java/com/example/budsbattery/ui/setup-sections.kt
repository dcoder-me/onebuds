package com.example.budsbattery.ui

import android.Manifest
import android.app.StatusBarManager
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.DeviceNotAssociatedException
import android.content.ComponentName
import android.content.Context
import android.content.IntentSender
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.budsbattery.R
import com.example.budsbattery.bt.buildBudsAssociationRequest
import com.example.budsbattery.bt.readBatteryOnce
import com.example.budsbattery.bt.saveAssociation
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.readSelectedAddress
import com.example.budsbattery.service.publishReading
import kotlinx.coroutines.launch

/** Lets the user pick the buds from the system's own device list. */
@Composable
fun SelectBudsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { address = readSelectedAddress(context) }

    val onPicked: (AssociationInfo) -> Unit = { info ->
        scope.launch {
            address = savePicked(context, info)
        }
    }
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val info = result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
        if (info != null) onPicked(info)
    }

    Column {
        Text(stringResource(if (address == null) R.string.no_buds_selected else R.string.buds_selected))
        Button(onClick = { startPicker(context, { request -> pickerLauncher.launch(request) }, onPicked) }) {
            Text(stringResource(R.string.select_buds))
        }
    }
}

/** Asks for the two permissions the app needs. */
@Composable
fun PermissionsSection() {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    Button(onClick = {
        launcher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS))
    }) {
        Text(stringResource(R.string.allow_permissions))
    }
}

/** Asks the system to add the tile to Quick Settings. */
@Composable
fun TileSection() {
    val context = LocalContext.current
    val label = stringResource(R.string.tile_label)
    Button(onClick = { requestTile(context, label) }) {
        Text(stringResource(R.string.add_tile))
    }
}

/** Reads the buds once and shows the result, so the hardware path can be checked by hand. */
@Composable
fun ReadNowSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    Button(onClick = { scope.launch { result = readNow(context) } }) {
        Text(stringResource(R.string.read_now))
    }
    result?.let { Text(it) }
}

/** Shows or hides the launcher icon. */
@Composable
fun IconSection() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(isLauncherIconVisible(context)) }
    Row {
        Text(stringResource(R.string.show_app_icon))
        Switch(
            checked = visible,
            onCheckedChange = { checked ->
                setLauncherIconVisible(context, checked)
                visible = checked
            },
        )
    }
}

private suspend fun savePicked(context: Context, info: AssociationInfo): String? = try {
    saveAssociation(context, info)
} catch (error: DeviceNotAssociatedException) {
    showToast(context, R.string.association_failed)
    null
}

private fun startPicker(context: Context, launch: (IntentSenderRequest) -> Unit, onPicked: (AssociationInfo) -> Unit) {
    val callback = object : CompanionDeviceManager.Callback() {
        override fun onAssociationPending(intentSender: IntentSender) {
            launch(IntentSenderRequest.Builder(intentSender).build())
        }

        override fun onAssociationCreated(associationInfo: AssociationInfo) {
            onPicked(associationInfo)
        }

        override fun onFailure(error: CharSequence?) {
            showToast(context, R.string.association_failed)
        }
    }
    context.getSystemService(CompanionDeviceManager::class.java)
        .associate(buildBudsAssociationRequest(), ContextCompat.getMainExecutor(context), callback)
}

private fun requestTile(context: Context, label: String) {
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, BudsTileService::class.java),
        label,
        Icon.createWithResource(context, R.drawable.ic_buds),
        ContextCompat.getMainExecutor(context),
    ) { result ->
        if (result != StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) showToast(context, R.string.add_tile_failed)
    }
}

private suspend fun readNow(context: Context): String? {
    val address = readSelectedAddress(context)
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    if (address == null || adapter == null) {
        showToast(context, R.string.select_buds_first)
        return null
    }
    val reading: BatteryReading? = readBatteryOnce(adapter, address)
    if (reading == null) {
        showToast(context, R.string.read_failed)
        return null
    }
    publishReading(context, reading)
    return formatBatteryLine(reading).ifEmpty { context.getString(R.string.no_reading) }
}

private fun showToast(context: Context, messageRes: Int) {
    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
}
