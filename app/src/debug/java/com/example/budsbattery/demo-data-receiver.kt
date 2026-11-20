package com.example.budsbattery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.budsbattery.data.BatteryLevel
import com.example.budsbattery.data.BatteryReading
import com.example.budsbattery.data.NoiseMode
import com.example.budsbattery.data.ReadSource
import com.example.budsbattery.data.saveBudInCase
import com.example.budsbattery.data.saveConnected
import com.example.budsbattery.data.saveNoiseMode
import com.example.budsbattery.data.saveReading
import com.example.budsbattery.service.refreshSurfaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val EXTRA_NOISE = "noise"
private const val EXTRA_KEEP_CONNECTION = "keep"
private val SAMPLE_READING = BatteryReading(
    left = BatteryLevel(percent = 82, charging = false),
    right = BatteryLevel(percent = 78, charging = false),
    case = BatteryLevel(percent = 64, charging = true),
)

/**
 * Debug-only: fills the store with sample buds so screenshots and demo videos work without real buds.
 *
 * @description Run `adb shell am broadcast -n com.example.budsbattery/.DemoDataReceiver --es noise TRANSPARENCY`.
 * Add `--ez keep true` to change only the noise mode and keep the current connection.
 */
class DemoDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        val noise = intent.getStringExtra(EXTRA_NOISE)
            ?.let { name -> NoiseMode.entries.firstOrNull { it.name == name } }
            ?: NoiseMode.NOISE_CANCELLING
        val keep = intent.getBooleanExtra(EXTRA_KEEP_CONNECTION, false)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!keep) {
                    saveConnected(app, connected = true)
                    saveReading(app, SAMPLE_READING, ReadSource.VENDOR_CHANNEL, System.currentTimeMillis())
                    saveBudInCase(app, budInCase = false)
                }
                saveNoiseMode(app, noise)
                refreshSurfaces(app)
            } finally {
                pending.finish()
            }
        }
    }
}
