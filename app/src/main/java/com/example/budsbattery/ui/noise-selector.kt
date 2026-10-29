package com.example.budsbattery.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.budsbattery.data.NoiseMode
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PILL_HEIGHT = 76.dp
private val PILL_INSET = 6.dp
private val ICON_SIZE = 28.dp
private const val DISABLED_ALPHA = 0.38f
private const val RESTING_ICON_SCALE = 0.86f

// Lands with a small overshoot, like the OnePlus sound-mode knob; low stiffness keeps the travel readable
private const val KNOB_DAMPING = 0.72f
private const val KNOB_STIFFNESS = 420f

// How far a fling carries the knob past where the finger let go, in seconds of travel at that speed
private const val FLING_CARRY_S = 0.08f

/**
 * OnePlus-style mode pill: three icons with a round knob that slides to the chosen one. Tap an icon, or drag the
 * knob and let go near one. The knob follows the finger, then springs into place.
 *
 * @param selected - mode to show as chosen; null hides the knob until the buds report one
 * @param enabled - false dims the pill and ignores input
 * @param onSelect - called once with the mode the user picked
 * @param modifier - layout modifier
 */
@Composable
fun NoiseSelector(selected: NoiseMode?, enabled: Boolean, onSelect: (NoiseMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = NoiseMode.entries
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(PILL_INSET),
    ) {
        val density = LocalDensity.current
        val slotWidth = with(density) { maxWidth.toPx() } / modes.size
        val knobSize = maxHeight
        val knobPx = with(density) { knobSize.toPx() }
        val knob = remember { Animatable((modes.indexOf(selected ?: NoiseMode.OFF)) * slotWidth) }
        var dragging by remember { mutableStateOf(false) }
        val haptics = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        // The icon under the knob lights up while dragging, so the finger always sees what it will pick
        val underKnob = nearestSlot(knob.value, slotWidth, modes.size)

        LaunchedEffect(selected, slotWidth) {
            if (selected != null && !dragging) knob.animateTo(modes.indexOf(selected) * slotWidth, knobSpring())
        }
        LaunchedEffect(underKnob) {
            if (dragging) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        val dragState = rememberDraggableState { delta ->
            scope.launch { knob.snapTo((knob.value + delta).coerceIn(0f, slotWidth * (modes.size - 1))) }
        }

        // Pops in once the buds report a mode, instead of guessing one
        val knobScale by animateFloatAsState(if (selected == null) 0f else 1f, knobSpring(), label = "knob")
        Box(
            modifier = Modifier
                .offset { IntOffset((knob.value + (slotWidth - knobPx) / 2).roundToInt(), 0) }
                .size(knobSize)
                .scale(knobScale)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled && selected != null,
                    onDragStarted = { dragging = true },
                    onDragStopped = { velocity ->
                        val pick = modes[nearestSlot(knob.value + velocity * FLING_CARRY_S, slotWidth, modes.size)]
                        knob.animateTo(modes.indexOf(pick) * slotWidth, knobSpring())
                        dragging = false
                        if (pick != selected) onSelect(pick)
                    },
                ),
        ) {
            for ((index, mode) in modes.withIndex()) {
                ModeSlot(mode, lit = selected != null && index == underKnob, enabled = enabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    if (mode != selected) onSelect(mode)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ModeSlot(mode: NoiseMode, lit: Boolean, enabled: Boolean, onClick: () -> Unit) {
    // Colour and size are effects, not movement, so they settle fast without bounce
    val tint by animateColorAsState(
        if (lit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "tint",
    )
    val iconScale by animateFloatAsState(if (lit) 1f else RESTING_ICON_SCALE, spring(stiffness = Spring.StiffnessMediumLow), label = "scale")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                enabled = enabled,
                role = Role.RadioButton,
                onClickLabel = noiseLabel(mode),
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(noiseIcon(mode)),
            contentDescription = noiseLabel(mode),
            tint = tint,
            modifier = Modifier.size(ICON_SIZE).scale(iconScale),
        )
    }
}

private fun nearestSlot(position: Float, slotWidth: Float, count: Int): Int =
    if (slotWidth <= 0f) 0 else (position / slotWidth).roundToInt().coerceIn(0, count - 1)

private fun <T> knobSpring() = spring<T>(dampingRatio = KNOB_DAMPING, stiffness = KNOB_STIFFNESS)
