package com.example.budsbattery.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.budsbattery.data.NoiseMode
import com.example.budsbattery.service.pickNoiseMode

private const val GLYPH_SHARE = 0.46f
private val CHIP_HEIGHT = 36.dp
private val WIDE_HEIGHT = 44.dp
private val SWITCH_RADIUS = 16.dp
private val SWITCH_LABEL_MIN_HEIGHT = 52.dp
private val SMALL_GLYPH = 18.dp
private val LABEL_GAP = 6.dp
private val modeKey = ActionParameters.Key<String>("mode")

// Runs in the app's process, where the open control channel picks the request up
class NoiseTapAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        pickNoiseMode(context.applicationContext, parameters[modeKey]?.let(NoiseMode::valueOf))
    }
}

/**
 * Adds the noise tap to a widget element, only while the controls work.
 *
 * @param noise - the widget's noise controls
 * @param mode - mode the tap picks; null steps to the next mode
 * @returns the modifier with the tap, or unchanged while the controls are off
 */
fun GlanceModifier.noiseTap(noise: NoiseControl, mode: NoiseMode? = null): GlanceModifier =
    if (!noise.enabled) this
    else clickable(actionRunCallback<NoiseTapAction>(mode?.let { actionParametersOf(modeKey to it.name) } ?: actionParametersOf()))

/**
 * A round button showing only the active mode's icon. A tap steps to the next mode.
 *
 * @param noise - the widget's noise controls
 * @param size - button side
 */
@Composable
fun NoiseRoundButton(noise: NoiseControl, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(size).cornerRadius(size / 2).background(if (noise.enabled) WIDGET_ACCENT else WIDGET_TRACK)
            .semantics { contentDescription = spokenOf(noise) }
            .noiseTap(noise),
    ) { NoiseGlyph(noise.mode, size * GLYPH_SHARE, if (noise.enabled) WIDGET_ON_ACCENT else WIDGET_MUTED) }
}

/**
 * A pill with the active mode's icon and short name, or the reason the controls are off. A tap steps to the next mode.
 *
 * @param noise - the widget's noise controls
 * @param wide - true fills the width and uses the full mode name
 */
@Composable
fun NoiseChip(noise: NoiseControl, wide: Boolean = false) {
    val mode = noise.mode
    val base = if (wide) GlanceModifier.fillMaxWidth().height(WIDE_HEIGHT) else GlanceModifier.height(CHIP_HEIGHT)
    Row(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
        modifier = base.cornerRadius(WIDE_HEIGHT / 2).background(if (noise.enabled) WIDGET_ACCENT else WIDGET_TRACK)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = spokenOf(noise) }
            .noiseTap(noise),
    ) {
        if (!noise.enabled || mode == null) {
            Text(noise.note.orEmpty(), style = WIDGET_NOTE)
            return@Row
        }
        NoiseGlyph(mode, SMALL_GLYPH, WIDGET_ON_ACCENT)
        Spacer(GlanceModifier.width(LABEL_GAP))
        Text(if (wide) noiseLabel(mode) else noiseShortLabel(mode), style = labelStyle(WIDGET_ON_ACCENT))
    }
}

/**
 * All three modes side by side, the active one lit. A tap picks that mode.
 *
 * @param noise - the widget's noise controls
 * @param height - row height; tall rows add the short name under each icon
 */
@Composable
fun NoiseSwitches(noise: NoiseControl, height: Dp) {
    if (!noise.enabled) {
        NoiseChip(noise, wide = true)
        return
    }
    Row(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        NoiseMode.entries.forEachIndexed { index, mode ->
            if (index > 0) Spacer(GlanceModifier.width(LABEL_GAP))
            NoiseSwitch(noise, mode, height, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun NoiseSwitch(noise: NoiseControl, mode: NoiseMode, height: Dp, modifier: GlanceModifier) {
    val active = mode == noise.mode
    val tint = if (active) WIDGET_ON_ACCENT else WIDGET_TEXT
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxHeight().cornerRadius(SWITCH_RADIUS).background(if (active) WIDGET_ACCENT else WIDGET_TRACK)
            .semantics { contentDescription = if (active) "${noiseLabel(mode)}, on" else noiseLabel(mode) }
            .noiseTap(noise, mode),
    ) {
        NoiseGlyph(mode, SMALL_GLYPH, tint)
        if (height >= SWITCH_LABEL_MIN_HEIGHT) Text(noiseShortLabel(mode), style = labelStyle(tint))
    }
}

/**
 * A mode icon, tinted.
 *
 * @param mode - mode to draw; null draws the off wave as a quiet stand-in
 * @param size - icon side
 * @param color - tint
 */
@Composable
fun NoiseGlyph(mode: NoiseMode?, size: Dp, color: ColorProvider) {
    Image(
        provider = ImageProvider(noiseIcon(mode ?: NoiseMode.OFF)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = GlanceModifier.size(size),
    )
}

/**
 * Says what the noise controls show, for screen readers.
 *
 * @param noise - the widget's noise controls
 * @returns such as "Noise cancelling. Tap for Off", or the reason the controls are off
 */
fun spokenOf(noise: NoiseControl): String {
    val mode = noise.mode
    if (!noise.enabled || mode == null) return noise.note.orEmpty()
    return "${noiseLabel(mode)}. Tap for ${noiseLabel(nextNoiseMode(mode))}"
}

private fun labelStyle(color: ColorProvider) = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
