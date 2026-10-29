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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R
import kotlin.math.roundToInt

private val GAUGE_SIZE = 92.dp
private val GAUGE_STROKE = 7.dp
private const val ICON_TO_GAUGE = 0.41f
private val BOX_GAUGE_SIZE = 64.dp
private val BOX_GAUGE_STROKE = 5.dp
private val BOX_CORNER = 24.dp
private val BOX_PADDING = 14.dp
private val BOX_GAP = 8.dp
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

/**
 * Left bud, right bud and case as three rounded boxes, each with its ring. Same rules as BudsPanel for what shows.
 *
 * @param view - what the screen shows
 * @param modifier - layout modifier
 */
@Composable
fun BudsBoxes(view: BudsView, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BOX_GAP)) {
        PartBox(visible = view.waiting || view.left != null) {
            PartGauge(R.drawable.ic_bud_left, stringResource(R.string.part_left), view.left, view.waiting, BOX_GAUGE_SIZE, BOX_GAUGE_STROKE)
        }
        PartBox(visible = view.waiting || view.right != null) {
            PartGauge(R.drawable.ic_bud_right, stringResource(R.string.part_right), view.right, view.waiting, BOX_GAUGE_SIZE, BOX_GAUGE_STROKE)
        }
        PartBox(visible = view.case != null) {
            PartGauge(R.drawable.ic_case, stringResource(R.string.part_case), view.case, waiting = false, BOX_GAUGE_SIZE, BOX_GAUGE_STROKE)
        }
    }
}

// The boxes share the row, so a box that leaves lets the others widen instead of leaving a hole
@Composable
private fun RowScope.PartBox(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
        modifier = Modifier.weight(1f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(BOX_CORNER))
                .padding(vertical = BOX_PADDING),
        ) { content() }
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
private fun PartGauge(
    iconRes: Int,
    label: String,
    part: PartView?,
    waiting: Boolean,
    gaugeSize: Dp = GAUGE_SIZE,
    stroke: Dp = GAUGE_STROKE,
) {
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(gaugeSize)) {
            if (waiting) {
                // Spins while this connection has no values yet, instead of showing old ones
                CircularProgressIndicator(modifier = Modifier.size(gaugeSize), strokeWidth = stroke)
            } else {
                CircularProgressIndicator(
                    progress = { fill.value },
                    modifier = Modifier.size(gaugeSize),
                    color = ringColor,
                    strokeWidth = stroke,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(gaugeSize * ICON_TO_GAUGE),
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
