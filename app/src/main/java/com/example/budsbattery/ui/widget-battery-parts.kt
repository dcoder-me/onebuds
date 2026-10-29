package com.example.budsbattery.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.budsbattery.R
import kotlin.math.roundToInt

private const val RING_GLYPH_SHARE = 0.42f
private const val FULL_PERCENT = 100f
private const val UNKNOWN = "—"
private const val BOLT = "⚡"
private val TILE_RADIUS = 14.dp
private val TILE_ICON = 18.dp
private val TILE_LABEL_MIN_HEIGHT = 72.dp
private val BAR_HEIGHT = 8.dp
private val BAR_ICON = 16.dp
private val BAR_VALUE = 44.dp

/** One battery the widgets can show on its own. */
enum class BudsPart { LEFT, RIGHT, CASE }

/**
 * A battery ring with only a picture in the middle; the arc shows the level and the label is spoken.
 *
 * @param view - what every surface shows
 * @param part - battery to show; null shows the one that runs out first, with the buds picture
 * @param size - ring side
 */
@Composable
fun BatteryRing(view: BudsView, part: BudsPart?, size: Dp) {
    val context = LocalContext.current
    val shown = part?.let { partOf(view, it) } ?: view.focus
    val glyph = part?.let(::partIcon) ?: shapeIcon(view.shape)
    val spec = RingSpec(shareOf(shown), isLow(shown), glyph, RING_GLYPH_SHARE)
    val colors = if (view.connected) DARK_RING_COLORS else AWAY_RING_COLORS
    val px = (size.value * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    Image(
        provider = ImageProvider(ringBitmap(context, px, spec, colors)),
        contentDescription = spokenOf(part?.let(::partName) ?: "Buds", shown),
        modifier = GlanceModifier.size(size),
    )
}

/**
 * A battery-shaped tile that fills from the bottom, with the part's picture, level and name.
 *
 * @param view - what every surface shows
 * @param part - battery to show
 * @param width - tile width
 * @param height - tile height
 */
@Composable
fun BatteryTile(view: BudsView, part: BudsPart, width: Dp, height: Dp) {
    val shown = partOf(view, part)
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = GlanceModifier.size(width, height).cornerRadius(TILE_RADIUS).background(WIDGET_TRACK),
    ) {
        Box(modifier = GlanceModifier.width(width).height(height * shareOf(shown)).background(fillOf(view, shown))) {}
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            PartIcon(part, TILE_ICON)
            Text(valueOf(shown), style = TextStyle(color = WIDGET_TEXT, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            if (height >= TILE_LABEL_MIN_HEIGHT) Text(partName(part), style = WIDGET_NOTE)
        }
    }
}

/**
 * One row with the part's picture, a level bar and the value.
 *
 * @param view - what every surface shows
 * @param part - battery to show
 * @param width - row width
 */
@Composable
fun BatteryBar(view: BudsView, part: BudsPart, width: Dp) {
    val shown = partOf(view, part)
    val track = (width - BAR_ICON - BAR_VALUE - WIDGET_GAP * 2).coerceAtLeast(0.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        PartIcon(part, BAR_ICON)
        Spacer(GlanceModifier.width(WIDGET_GAP))
        Box(modifier = GlanceModifier.width(track).height(BAR_HEIGHT).cornerRadius(BAR_HEIGHT / 2).background(WIDGET_TRACK)) {
            Box(
                modifier = GlanceModifier.width(track * shareOf(shown)).height(BAR_HEIGHT).cornerRadius(BAR_HEIGHT / 2)
                    .background(barColorOf(view, shown)),
            ) {}
        }
        Spacer(GlanceModifier.width(WIDGET_GAP))
        Text(valueOf(shown), style = TextStyle(color = WIDGET_TEXT, fontSize = 13.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.width(BAR_VALUE))
    }
}

@Composable
private fun PartIcon(part: BudsPart, size: Dp) {
    Image(
        provider = ImageProvider(partIcon(part)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(WIDGET_TEXT),
        modifier = GlanceModifier.size(size),
    )
}

private fun partOf(view: BudsView, part: BudsPart): PartView? = when (part) {
    BudsPart.LEFT -> view.left
    BudsPart.RIGHT -> view.right
    BudsPart.CASE -> view.case
}

private fun partIcon(part: BudsPart): Int = when (part) {
    BudsPart.LEFT -> R.drawable.ic_bud_left
    BudsPart.RIGHT -> R.drawable.ic_bud_right
    BudsPart.CASE -> R.drawable.ic_case
}

private fun partName(part: BudsPart): String = when (part) {
    BudsPart.LEFT -> "Left"
    BudsPart.RIGHT -> "Right"
    BudsPart.CASE -> "Case"
}

private fun shareOf(shown: PartView?): Float = (shown?.level?.percent ?: 0) / FULL_PERCENT

private fun isLow(shown: PartView?): Boolean = shown != null && shown.level.percent <= LOW_PERCENT

private fun valueOf(shown: PartView?): String =
    shown?.let { "${it.level.percent}%${if (it.level.charging) BOLT else ""}" } ?: UNKNOWN

private fun spokenOf(name: String, shown: PartView?): String =
    shown?.let { "$name ${it.level.percent}%" } ?: "$name not read"

private fun fillOf(view: BudsView, shown: PartView?) = when {
    !view.connected -> WIDGET_FILL_AWAY
    isLow(shown) -> WIDGET_FILL_LOW
    else -> WIDGET_FILL
}

private fun barColorOf(view: BudsView, shown: PartView?) = when {
    !view.connected -> WIDGET_AWAY
    isLow(shown) -> WIDGET_LOW
    else -> WIDGET_ACCENT
}
