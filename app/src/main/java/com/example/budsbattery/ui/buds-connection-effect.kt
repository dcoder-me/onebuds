package com.example.budsbattery.ui

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

// Sent once the audio profile itself changes, so a lookup on it already sees the new state
private val PROFILE_STATE_ACTIONS = listOf(
    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
    BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED,
)

/**
 * Calls back whenever an audio device connects or disconnects, for as long as the caller is on screen.
 *
 * @description Detecting only when the screen opened missed buds that connected or left while it stayed open,
 * so the chip kept saying "Detect buds" or kept showing buds that were gone.
 * @param onChange - called with the device address and its new profile state on every connect or disconnect
 */
@Composable
fun BudsConnectionEffect(onChange: (address: String?, state: Int) -> Unit) {
    val context = LocalContext.current
    val currentOnChange = rememberUpdatedState(onChange)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                currentOnChange.value(device?.address, state)
            }
        }
        val filter = IntentFilter().apply { for (action in PROFILE_STATE_ACTIONS) addAction(action) }
        // These are protected broadcasts only the system can send, so exporting is safe
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
}
