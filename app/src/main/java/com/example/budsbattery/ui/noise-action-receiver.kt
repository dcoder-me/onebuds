package com.example.budsbattery.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.budsbattery.service.pickNoiseMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Steps the buds to the next noise mode when the Live Update's mode button is tapped
class NoiseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pickNoiseMode(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Builds the tap action for the Live Update's noise button.
 *
 * @param context - app context
 * @returns a broadcast that steps to the next mode
 */
fun nextNoiseModeIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, NoiseActionReceiver::class.java),
    PendingIntent.FLAG_IMMUTABLE,
)
