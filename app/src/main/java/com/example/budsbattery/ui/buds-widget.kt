package com.example.budsbattery.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.budsbattery.R
import com.example.budsbattery.data.readBatteryState

private val WIDGET_PADDING = 16.dp

/** Home screen widget with the same values as the tile. */
class BudsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = readBatteryState(context)
        provideContent { BudsWidgetContent(formatTileSubtitle(state, System.currentTimeMillis())) }
    }
}

@Composable
private fun BudsWidgetContent(subtitle: String) {
    val title = LocalContext.current.getString(R.string.tile_label)
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(WIDGET_PADDING)
                .background(GlanceTheme.colors.widgetBackground),
        ) {
            Text(text = title, style = TextStyle(color = GlanceTheme.colors.onSurface))
            Text(text = subtitle, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        }
    }
}
