package com.example.budsbattery.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.batteryStateFlow
import com.example.budsbattery.service.startBatteryRead
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_CLOSE_MS = 6_000L
private const val SLIDE_MS = 350
private const val CLOCK_TICK_MS = 30_000L
private const val FLING_CLOSE_VELOCITY = 1_200f
private const val HANDLE_ALPHA = 0.4f
private val CARD_MARGIN = 12.dp
private val CARD_PADDING = 20.dp
private val CARD_CORNER = 32.dp
private val CARD_ELEVATION = 8.dp
private val HANDLE_WIDTH = 32.dp
private val HANDLE_HEIGHT = 4.dp
private val SWIPE_CLOSE_DISTANCE = 72.dp

/**
 * AirPods-style card that slides up from the bottom. Opened from the tile or the notification.
 * No backdrop and no buttons: swipe it down or tap outside to close; it hides by itself after a few seconds.
 */
class BudsPopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The card animates itself; skip the window's own open and close animations
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        // Ask for fresh values now (also restarts a sync that died)
        startBatteryRead(applicationContext, refresh = true)
        setContent {
            BudsTheme { PopupScreen(onClosed = ::finish) }
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


@Composable
private fun PopupScreen(onClosed: () -> Unit) {
    val context = LocalContext.current
    val state by remember { batteryStateFlow(context.applicationContext) }
        .collectAsState(initial = BatteryState(snapshot = null, connected = false))
    // "read 2h ago" keeps counting while the card is open
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shown = true
        delay(AUTO_CLOSE_MS)
        shown = false
    }
    LaunchedEffect(shown) {
        if (!shown) {
            delay(SLIDE_MS.toLong())
            onClosed()
        }
    }

    // No backdrop: the app behind stays as it is, and a tap anywhere outside the card closes it
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { shown = false },
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(tween(SLIDE_MS)) { it } + fadeIn(),
            exit = slideOutVertically(tween(SLIDE_MS)) { it } + fadeOut(),
        ) {
            PopupCard(view = budsViewOf(state, now), onDismiss = { shown = false })
        }
    }
}

@Composable
private fun PopupCard(view: BudsView, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val closeDistance = with(LocalDensity.current) { SWIPE_CLOSE_DISTANCE.toPx() }
    val dragState = rememberDraggableState { delta ->
        scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
    }
    Card(
        shape = RoundedCornerShape(CARD_CORNER),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        // With no backdrop, a soft shadow is what lifts the card off the app behind it
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(CARD_MARGIN)
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (dragOffset.value > closeDistance || velocity > FLING_CLOSE_VELOCITY) onDismiss() else dragOffset.animateTo(0f)
                },
            )
            // Taps on the card must not fall through and close it
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            Column {
                Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(view.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BudsPanel(view)
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(HANDLE_WIDTH, HANDLE_HEIGHT)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HANDLE_ALPHA), RoundedCornerShape(HANDLE_HEIGHT / 2)),
    )
}
