package com.example.budsbattery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R
import kotlin.math.roundToInt

private val GAUGE_SIZE = 92.dp
private val GAUGE_STROKE = 7.dp
private val PART_ICON_SIZE = 38.dp
private const val FULL_PERCENT = 100f
private const val TRACK_ALPHA = 0.14f
private const val OLD_ALPHA = 0.55f
private const val FILL_MS = 900
private const val PULSE_MS = 700
private const val PULSE_MIN_ALPHA = 0.3f

/**
 * Left bud, right bud and case side by side, each with a ring that fills up. A bud out of reach
 * slides away, so one bud in the ear shows one bud; the case shows only while a bud sits in it.
 *
 * @param view - what the screen shows
 * @param modifier - layout modifier
 */
@Composable
fun BudsPanel(view: BudsView, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        PartSlot(visible = view.waiting || view.left != null) {
            PartGauge(R.drawable.ic_bud_left, stringResource(R.string.part_left), view.left, view.waiting)
        }
        PartSlot(visible = view.waiting || view.right != null) {
            PartGauge(R.drawable.ic_bud_right, stringResource(R.string.part_right), view.right, view.waiting)
        }
        PartSlot(visible = view.case != null) {
            PartGauge(R.drawable.ic_case, stringResource(R.string.part_case), view.case, waiting = false)
        }
    }
}

@Composable
private fun PartSlot(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) { content() }
}

@Composable
private fun PartGauge(iconRes: Int, label: String, part: PartView?, waiting: Boolean) {
    val percent = part?.level?.percent ?: 0
    // Starts empty and fills to the level, like the AirPods card; refills smoothly on every change
    val fill = remember { Animatable(0f) }
    LaunchedEffect(percent) { fill.animateTo(percent / FULL_PERCENT, tween(FILL_MS, easing = FastOutSlowInEasing)) }
    val ringColor = if (percent <= LOW_PERCENT && part != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.alpha(if (part == null || part.live) 1f else OLD_ALPHA),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(GAUGE_SIZE)) {
            if (waiting) {
                // Spins while this connection has no values yet, instead of showing old ones
                CircularProgressIndicator(modifier = Modifier.size(GAUGE_SIZE), strokeWidth = GAUGE_STROKE)
            } else {
                CircularProgressIndicator(
                    progress = { fill.value },
                    modifier = Modifier.size(GAUGE_SIZE),
                    color = ringColor,
                    strokeWidth = GAUGE_STROKE,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(PART_ICON_SIZE),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (part?.level?.charging == true) ChargingBolt()
            Text(
                text = if (part == null) stringResource(R.string.unknown_level) else "${(fill.value * FULL_PERCENT).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChargingBolt() {
    val pulse = rememberInfiniteTransition(label = "charging")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
        label = "bolt",
    )
    Text("⚡", color = MaterialTheme.colorScheme.primary, modifier = Modifier.alpha(alpha))
}
