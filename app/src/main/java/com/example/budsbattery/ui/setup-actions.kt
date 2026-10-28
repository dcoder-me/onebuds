package com.example.budsbattery.ui

import android.Manifest
import android.app.StatusBarManager
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.DeviceNotAssociatedException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.budsbattery.R
import com.example.budsbattery.bt.buildBudsAssociationRequest
import com.example.budsbattery.bt.readBatteryOnce
import com.example.budsbattery.bt.saveAssociation
import com.example.budsbattery.data.readSelectedAddress
import com.example.budsbattery.service.hasBluetoothPermission
import com.example.budsbattery.service.publishReading

/** Permissions asked for in setup. */
val SETUP_PERMISSIONS = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)

/**
 * Tells whether every setup permission is granted.
 *
 * @param context - any context
 * @returns true when Bluetooth and notifications are both allowed
 */
fun hasSetupPermissions(context: Context): Boolean = SETUP_PERMISSIONS.all { permission ->
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Opens this app's page in system Settings, for when Android stops showing the permission dialog.
 *
 * @param context - activity context
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}

/**
 * Saves the picked buds. Shows a toast instead of crashing if the system lost the association.
 *
 * @param context - app context
 * @param info - association from the picker
 * @returns the saved MAC address, or null on failure
 */
suspend fun savePickedBuds(context: Context, info: AssociationInfo): String? = try {
    saveAssociation(context, info)
} catch (error: DeviceNotAssociatedException) {
    showToast(context, R.string.association_failed)
    null
}

/**
 * Opens the system device picker.
 *
 * @param context - activity context
 * @param launch - starts the picker screen Android hands back
 * @param onPicked - called with the new association
 */
fun startBudsPicker(context: Context, launch: (IntentSender) -> Unit, onPicked: (AssociationInfo) -> Unit) {
    val callback = object : CompanionDeviceManager.Callback() {
        override fun onAssociationPending(intentSender: IntentSender) = launch(intentSender)

        override fun onAssociationCreated(associationInfo: AssociationInfo) = onPicked(associationInfo)

        override fun onFailure(error: CharSequence?) = showToast(context, R.string.association_failed)
    }
    context.getSystemService(CompanionDeviceManager::class.java)
        .associate(buildBudsAssociationRequest(), ContextCompat.getMainExecutor(context), callback)
}

/**
 * Asks the system to add the tile to Quick Settings, then toasts the result.
 *
 * @param context - activity context
 */
fun requestTile(context: Context) {
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, BudsTileService::class.java),
        context.getString(R.string.tile_label),
        Icon.createWithResource(context, R.drawable.ic_buds),
        ContextCompat.getMainExecutor(context),
    ) { result ->
        val added = result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
            result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
        showToast(context, if (added) R.string.tile_added else R.string.add_tile_failed)
    }
}

/**
 * Reads the buds once and saves the values. The battery card updates from the saved state.
 *
 * @param context - app context
 */
suspend fun readNow(context: Context) {
    val address = readSelectedAddress(context)
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    when {
        !hasBluetoothPermission(context) -> showToast(context, R.string.allow_permissions_first)
        address == null || adapter == null -> showToast(context, R.string.select_buds_first)
        else -> {
            val reading = readBatteryOnce(adapter, address)
            if (reading == null) showToast(context, R.string.read_failed) else publishReading(context, reading)
        }
    }
}

/**
 * Shows a short toast.
 *
 * @param context - any context
 * @param messageRes - string resource to show
 */
fun showToast(context: Context, messageRes: Int) {
    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
}
