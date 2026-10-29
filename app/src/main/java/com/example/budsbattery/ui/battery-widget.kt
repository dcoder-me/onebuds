package com.example.budsbattery.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.example.budsbattery.data.readBatteryState

private val MAX_RING = 72.dp
private const val CASE_FROM_COLUMNS = 3

/** Battery-only widget: one ring, tiles, bars or tall tiles by size. A tap opens the battery popup. */
class BatteryWidget : GlanceAppWidget() {
    // Exact sizing redraws for the real size, so every fill matches its level
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = readBatteryState(context)
        provideContent { BatteryWidgetContent(budsViewOf(state, System.currentTimeMillis())) }
    }
}

@Composable
private fun BatteryWidgetContent(view: BudsView) {
    val size = LocalSize.current
    val grid = widgetGridOf(size.width.value, size.height.value)
    WidgetCard(openPopup(LocalContext.current)) {
        when (batteryLayoutOf(grid)) {
            BatteryLayout.RING -> BatteryRing(view, part = null, size = minOf(innerOf(size.width), innerOf(size.height), MAX_RING))
            BatteryLayout.BARS -> BarsLayout(view)
            BatteryLayout.TILES, BatteryLayout.TALL_TILES -> TilesLayout(view, withCase = grid.columns >= CASE_FROM_COLUMNS)
        }
    }
}

@Composable
private fun TilesLayout(view: BudsView, withCase: Boolean) {
    val size = LocalSize.current
    val parts = listOfNotNull(BudsPart.LEFT, BudsPart.RIGHT, BudsPart.CASE.takeIf { withCase })
    val tileWidth = (innerOf(size.width) - WIDGET_GAP * (parts.size - 1)) / parts.size
    Row(modifier = GlanceModifier.fillMaxSize()) {
        parts.forEachIndexed { index, part ->
            if (index > 0) Spacer(GlanceModifier.width(WIDGET_GAP))
            BatteryTile(view, part, tileWidth, innerOf(size.height))
        }
    }
}

@Composable
private fun BarsLayout(view: BudsView) {
    val width = innerOf(LocalSize.current.width)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        listOf(BudsPart.LEFT, BudsPart.RIGHT, BudsPart.CASE).forEach { part ->
            Spacer(GlanceModifier.defaultWeight())
            BatteryBar(view, part, width)
        }
        Spacer(GlanceModifier.defaultWeight().height(0.dp))
    }
}
