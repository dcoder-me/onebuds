package com.example.budsbattery.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.example.budsbattery.data.readBatteryState

private val LABEL_ROW = 20.dp
private val MIN_BUTTON = 24.dp
private val MAX_BUTTON = 64.dp
private val MODE_BUTTON = 48.dp
private val MAX_SWITCHES = 72.dp

/** Noise-only widget: one cycling button, the mode with its name, or all three switches by size. */
class NoiseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = readBatteryState(context)
        provideContent { NoiseWidgetContent(noiseControlOf(state, System.currentTimeMillis())) }
    }
}

@Composable
private fun NoiseWidgetContent(noise: NoiseControl) {
    val size = LocalSize.current
    val layout = noiseLayoutOf(widgetGridOf(size.width.value, size.height.value))
    // Small sizes show only the active mode, so the whole widget steps to the next one
    WidgetCard(if (layout == NoiseLayout.SWITCHES) GlanceModifier else GlanceModifier.noiseTap(noise)) {
        when (layout) {
            NoiseLayout.CYCLE -> CycleLayout(noise)
            NoiseLayout.MODE -> ModeLayout(noise)
            NoiseLayout.SWITCHES -> NoiseSwitches(noise, innerOf(size.height).coerceAtMost(MAX_SWITCHES))
        }
    }
}

@Composable
private fun CycleLayout(noise: NoiseControl) {
    val size = LocalSize.current
    val mode = noise.mode
    val button = minOf(innerOf(size.width), innerOf(size.height) - LABEL_ROW).coerceIn(MIN_BUTTON, MAX_BUTTON)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        NoiseRoundButton(noise, button)
        Text(if (noise.enabled && mode != null) noiseShortLabel(mode) else "—", style = WIDGET_NOTE)
    }
}

@Composable
private fun ModeLayout(noise: NoiseControl) {
    val mode = noise.mode
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
        NoiseRoundButton(noise, MODE_BUTTON)
        Spacer(GlanceModifier.width(WIDGET_GAP))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(if (noise.enabled && mode != null) noiseLabel(mode) else "Noise control", style = WIDGET_TITLE, maxLines = 1)
            Text(if (noise.enabled && mode != null) noiseHint(mode) else noise.note.orEmpty(), style = WIDGET_NOTE, maxLines = 1)
        }
    }
}
