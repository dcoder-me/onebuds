package com.example.budsbattery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.budsbattery.data.BatteryState
import com.example.budsbattery.data.NoiseMode
import com.example.budsbattery.data.batteryStateFlow
import com.example.budsbattery.service.requestNoiseMode
import kotlinx.coroutines.delay

private const val TITLE = "Noise control"
private const val CONNECT_FIRST = "Connect your buds first"
private const val LABEL_MS = 220

// A pick shows at once; if the buds haven't confirmed it by then, the knob returns to what they report
private const val PICK_TIMEOUT_MS = 2_500L

/**
 * Noise control card: the mode pill and the name of the chosen mode.
 */
@Composable
fun NoiseCard() {
    val context = LocalContext.current
    val state by remember { batteryStateFlow(context.applicationContext) }
        .collectAsState(initial = BatteryState(snapshot = null, connected = false))
    val reported = noiseModeOf(state)
    var pending by remember { mutableStateOf<NoiseMode?>(null) }
    LaunchedEffect(pending, reported) {
        if (pending == null) return@LaunchedEffect
        if (pending != reported) delay(PICK_TIMEOUT_MS)
        pending = null
    }
    val shown = pending ?: reported

    PanelCardHeader(TITLE, formatNoiseSubtitle(state).takeIf { state.connected } ?: CONNECT_FIRST)
    NoiseSelector(
        selected = shown,
        enabled = state.connected,
        onSelect = { mode ->
            pending = mode
            requestNoiseMode(mode)
        },
    )
    ModeName(shown)
}

// The name slides in from the side the knob moved toward, so the text follows the motion
@Composable
private fun ModeName(mode: NoiseMode?) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val direction = if ((targetState?.ordinal ?: 0) >= (initialState?.ordinal ?: 0)) 1 else -1
            (slideInHorizontally(tween(LABEL_MS)) { it / 4 * direction } + fadeIn(tween(LABEL_MS))) togetherWith
                (slideOutHorizontally(tween(LABEL_MS)) { -it / 4 * direction } + fadeOut(tween(LABEL_MS)))
        },
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth(),
        label = "mode name",
    ) { target ->
        Text(
            text = target?.let(::noiseLabel).orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
