package com.example.budsbattery.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow
import com.example.budsbattery.service.startBatteryRead
import kotlinx.coroutines.delay

private const val TAG = "BudsPopup"
private const val EXTRA_AUTO = "com.example.budsbattery.extra.AUTO"
private const val AUTO_CLOSE_MS = 7_000L
private const val SLIDE_MS = 350
private const val CLOCK_TICK_MS = 30_000L
private const val SCRIM_ALPHA = 0.32f
private val CARD_MARGIN = 12.dp
private val CARD_PADDING = 20.dp
private val CARD_CORNER = 32.dp
private val HEADER_ICON_SIZE = 28.dp

/** AirPods-style card that slides up from the bottom. Opened from the tile, the notification, or on connect. */
class BudsPopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The card animates itself; skip the window's own open and close animations
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        val auto = intent.getBooleanExtra(EXTRA_AUTO, false)
        // Opened by hand: ask for fresh values now (also restarts a sync that died)
        if (!auto) startBatteryRead(applicationContext, refresh = true)
        setContent {
            BudsTheme { PopupScreen(auto = auto, onClosed = ::finish) }
        }
    }
}

/**
 * Builds the intent the tile and notification use to open the card.
 *
 * @param context - any context
 * @returns an immutable pending intent
 */
fun budsPopupIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, BudsPopupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_IMMUTABLE,
)

/**
 * Tells whether the card may pop up by itself when the buds connect.
 *
 * @param context - any context
 * @returns true when "Display over other apps" is allowed
 */
fun canPopUpOnConnect(context: Context): Boolean = Settings.canDrawOverlays(context)

/**
 * Pops the card up by itself, like the AirPods case-open card. Closes after a few seconds.
 *
 * @description Android blocks activity starts from the background unless "Display over other apps" is allowed.
 * @param context - service context
 */
fun showPopupOnConnect(context: Context) {
    if (!canPopUpOnConnect(context)) {
        Log.d(TAG, "Display over other apps not allowed; no pop-up")
        return
    }
    val intent = Intent(context, BudsPopupActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(EXTRA_AUTO, true)
    try {
        context.startActivity(intent)
    } catch (error: RuntimeException) {
        Log.w(TAG, "Android blocked the pop-up", error)
    }
}

@Composable
private fun PopupScreen(auto: Boolean, onClosed: () -> Unit) {
    val context = LocalContext.current
    val state by remember { batteryStateFlow(context.applicationContext) }
        .collectAsState(initial = BatteryState(snapshot = null, connected = false))
    // Ages like "seen 5 min ago" keep counting while the card is open
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(auto) {
        if (auto) {
            delay(AUTO_CLOSE_MS)
            shown = false
        }
    }
    LaunchedEffect(shown) {
        if (!shown) {
            delay(SLIDE_MS.toLong())
            onClosed()
        }
    }
    val scrim by animateFloatAsState(if (shown) SCRIM_ALPHA else 0f, tween(SLIDE_MS), label = "scrim")

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrim))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { shown = false },
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(tween(SLIDE_MS)) { it } + fadeIn(),
            exit = slideOutVertically(tween(SLIDE_MS)) { it } + fadeOut(),
        ) {
            PopupCard(view = budsViewOf(state, now), onDone = { shown = false })
        }
    }
}

@Composable
private fun PopupCard(view: BudsView, onDone: () -> Unit) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(CARD_CORNER),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(CARD_MARGIN)
            // Taps on the card must not fall through to the scrim and close it
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(shapeIcon(view.shape)), contentDescription = null, modifier = Modifier.size(HEADER_ICON_SIZE))
                Column {
                    Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.titleLarge)
                    Text(view.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            BudsPanel(view)
            view.sourceNote?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = { openSetup(context) }) { Text(stringResource(R.string.action_settings)) }
                if (view.connected) {
                    TextButton(onClick = { startBatteryRead(context.applicationContext, refresh = true) }) {
                        Text(stringResource(R.string.action_refresh))
                    }
                }
                FilledTonalButton(onClick = onDone) { Text(stringResource(R.string.action_done)) }
            }
        }
    }
}

private fun openSetup(context: Context) {
    context.startActivity(Intent(context, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
