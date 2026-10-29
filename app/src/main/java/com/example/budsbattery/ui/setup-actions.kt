package com.example.budsbattery.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.DeviceNotAssociatedException
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.budsbattery.R
import com.example.budsbattery.bt.PairedDevice
import com.example.budsbattery.bt.associationAddress
import com.example.budsbattery.bt.buildBudsAssociationRequest
import com.example.budsbattery.bt.findConnectedBuds
import com.example.budsbattery.bt.saveAssociation
import com.example.budsbattery.data.saveSelectedAddress
import com.example.budsbattery.service.hasBluetoothPermission

/** Permissions asked for in setup. */
val SETUP_PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.POST_NOTIFICATIONS,
)

/**
 * Tells whether every setup permission is granted.
 *
 * @param context - any context
 * @returns true when Bluetooth, nearby scan and notifications are all allowed
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
 * Finds the buds connected right now and saves them as the device to read. No list to pick from.
 *
 * @param context - app context
 * @returns the connected buds, or null when none is connected or permission is missing
 */
suspend fun detectBuds(context: Context): PairedDevice? {
    val adapter = adapterOf(context) ?: return null
    if (!hasBluetoothPermission(context)) return null
    val buds = findConnectedBuds(context, adapter) ?: return null
    saveSelectedAddress(context, buds.address)
    return buds
}

/**
 * Tells whether Android already wakes the app for these buds.
 *
 * @param context - any context
 * @param address - MAC address of the buds
 * @returns true when an association for this address exists
 */
fun isLinked(context: Context, address: String): Boolean =
    context.getSystemService(CompanionDeviceManager::class.java).myAssociations
        .any { info -> associationAddress(info).equals(address, ignoreCase = true) }

/**
 * Saves a new association so Android wakes the app when the buds connect.
 *
 * @param context - app context
 * @param info - association from the link step
 */
suspend fun saveLink(context: Context, info: AssociationInfo) {
    try {
        saveAssociation(context, info)
    } catch (error: DeviceNotAssociatedException) {
        showToast(context, R.string.auto_read_failed)
    }
}

/**
 * Asks Android to link the app to the buds, so it wakes the app when they connect.
 *
 * @param context - activity context
 * @param address - MAC address of the connected buds
 * @param launch - starts the confirm screen Android hands back
 * @param onLinked - called with the new association
 */
fun linkBuds(context: Context, address: String, launch: (IntentSender) -> Unit, onLinked: (AssociationInfo) -> Unit) {
    val callback = object : CompanionDeviceManager.Callback() {
        override fun onAssociationPending(intentSender: IntentSender) = launch(intentSender)

        override fun onAssociationCreated(associationInfo: AssociationInfo) = onLinked(associationInfo)

        override fun onFailure(error: CharSequence?) = showToast(context, R.string.auto_read_failed)
    }
    context.getSystemService(CompanionDeviceManager::class.java)
        .associate(buildBudsAssociationRequest(address), ContextCompat.getMainExecutor(context), callback)
}

/**
 * Tells whether Android 16 lets the battery notification be pinned at the top as a Live Update.
 *
 * @param context - any context
 * @returns true when promoted notifications are allowed for this app
 */
fun canPinToTop(context: Context): Boolean = NotificationManagerCompat.from(context).canPostPromotedNotifications()

/**
 * Opens the system page where the user allows Live Updates for this app.
 *
 * @param context - activity context
 */
fun openPinSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    // Some phones lack this page; the app's notification settings are the closest fallback
    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
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

private fun adapterOf(context: Context): BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
