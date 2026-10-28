package com.example.budsbattery.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

private const val LAUNCHER_ALIAS_SUFFIX = ".LauncherAlias"

/**
 * Tells whether the launcher icon is showing.
 *
 * @param context - app context
 * @returns false only after the icon was hidden
 */
fun isLauncherIconVisible(context: Context): Boolean =
    context.packageManager.getComponentEnabledSetting(launcherAlias(context)) !=
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED

/**
 * Shows or hides the launcher icon. The tile and the read service keep working either way.
 *
 * @param context - app context
 * @param visible - true to show the icon, false to hide it
 */
fun setLauncherIconVisible(context: Context, visible: Boolean) {
    val state = if (visible) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    // DONT_KILL_APP keeps the read service running while the icon is hidden
    context.packageManager.setComponentEnabledSetting(launcherAlias(context), state, PackageManager.DONT_KILL_APP)
}

private fun launcherAlias(context: Context): ComponentName =
    ComponentName(context.packageName, "${context.packageName}$LAUNCHER_ALIAS_SUFFIX")
