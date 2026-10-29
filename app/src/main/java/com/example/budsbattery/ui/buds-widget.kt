package com.example.budsbattery.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.example.budsbattery.data.readBatteryState

private val MAX_RING = 56.dp
private val SMALL_RING = 44.dp
private val ROUND_BUTTON = 52.dp
private val SWITCHES_HEIGHT = 56.dp
private val LABEL_ROW = 20.dp
private val SMALL_GLYPH = 14.dp

/** Battery + noise widget. Each size has its own approved layout. */
class BudsWidget : GlanceAppWidget() {
    // Exact sizing redraws for the real size, so every fill and ring fits the space it gets
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = readBatteryState(context)
        val now = System.currentTimeMillis()
        provideContent { BudsWidgetContent(budsViewOf(state, now), noiseControlOf(state, now)) }
    }
}

@Composable
private fun BudsWidgetContent(view: BudsView, noise: NoiseControl) {
    val size = LocalSize.current
    val layout = budsLayoutOf(widgetGridOf(size.width.value, size.height.value))
    // The smallest size is one button: the whole widget steps the mode. Larger ones open the battery popup
    val tap = if (layout == BudsLayout.RING) GlanceModifier.noiseTap(noise) else openPopup(LocalContext.current)
    WidgetCard(tap) {
        when (layout) {
            BudsLayout.RING -> RingLayout(view, noise)
            BudsLayout.RING_BUTTON -> RingButtonLayout(view, noise)
            BudsLayout.PAIR_BUTTON -> RingsRow(view, listOf(BudsPart.LEFT, BudsPart.RIGHT)) { NoiseRoundButton(noise, ROUND_BUTTON) }
            BudsLayout.TRIO_CHIP -> RingsRow(view, listOf(BudsPart.LEFT, BudsPart.RIGHT, BudsPart.CASE)) { NoiseChip(noise) }
            BudsLayout.SQUARE -> SquareLayout(view, noise)
            BudsLayout.WIDE -> WideLayout(view, noise)
        }
    }
}

/**
 * Taps on the widget body open the battery popup.
 *
 * @param context - widget context
 * @returns a modifier that opens the popup
 */
fun openPopup(context: Context): GlanceModifier =
    GlanceModifier.clickable(actionStartActivity(Intent(context, BudsPopupActivity::class.java)))

@Composable
private fun RingLayout(view: BudsView, noise: NoiseControl) {
    val size = LocalSize.current
    val ring = minOf(innerOf(size.width), innerOf(size.height) - LABEL_ROW).coerceIn(SMALL_GLYPH, MAX_RING)
    val mode = noise.mode
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        BatteryRing(view, part = null, size = ring)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.height(LABEL_ROW)) {
            NoiseGlyph(mode, SMALL_GLYPH, if (noise.enabled) WIDGET_ACCENT else WIDGET_MUTED)
            Spacer(GlanceModifier.width(4.dp))
            Text(if (noise.enabled && mode != null) noiseShortLabel(mode) else "—", style = WIDGET_NOTE)
        }
    }
}

@Composable
private fun RingButtonLayout(view: BudsView, noise: NoiseControl) {
    val mode = noise.mode
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
        BatteryRing(view, part = null, size = ringFor(LocalSize.current.height))
        Spacer(GlanceModifier.width(WIDGET_GAP))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text("Buds", style = WIDGET_TITLE, maxLines = 1)
            Text(if (noise.enabled && mode != null) noiseLabel(mode) else noise.note.orEmpty(), style = WIDGET_NOTE, maxLines = 1)
        }
        NoiseRoundButton(noise, ROUND_BUTTON)
    }
}

@Composable
private fun RingsRow(view: BudsView, parts: List<BudsPart>, control: @Composable () -> Unit) {
    val ring = ringFor(LocalSize.current.height)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
        parts.forEach { part ->
            BatteryRing(view, part, ring)
            Spacer(GlanceModifier.width(WIDGET_GAP))
        }
        Spacer(GlanceModifier.defaultWeight())
        control()
    }
}

@Composable
private fun SquareLayout(view: BudsView, noise: NoiseControl) {
    val width = innerOf(LocalSize.current.width)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BatteryRing(view, BudsPart.LEFT, SMALL_RING)
            Spacer(GlanceModifier.width(WIDGET_GAP))
            BatteryRing(view, BudsPart.RIGHT, SMALL_RING)
        }
        Spacer(GlanceModifier.defaultWeight())
        BatteryBar(view, BudsPart.CASE, width)
        Spacer(GlanceModifier.defaultWeight())
        NoiseChip(noise, wide = true)
    }
}

@Composable
private fun WideLayout(view: BudsView, noise: NoiseControl) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.fillMaxWidth()) {
            listOf(BudsPart.LEFT, BudsPart.RIGHT, BudsPart.CASE).forEachIndexed { index, part ->
                if (index > 0) Spacer(GlanceModifier.width(WIDGET_GAP * 3))
                BatteryRing(view, part, SMALL_RING)
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        NoiseSwitches(noise, SWITCHES_HEIGHT)
    }
}

private fun ringFor(height: Dp): Dp = innerOf(height).coerceIn(SMALL_GLYPH, MAX_RING)
