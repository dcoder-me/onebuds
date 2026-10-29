package com.example.budsbattery.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// === NOTE: === Widgets keep the approved dark OnePlus card in light mode too, like the system's own device widgets
val WIDGET_CARD = ColorProvider(Color(0xFF1C2522))
val WIDGET_TEXT = ColorProvider(Color(0xFFE8EFEC))
val WIDGET_MUTED = ColorProvider(Color(0xFF9AAAA5))
val WIDGET_ACCENT = ColorProvider(Color(0xFF6FD1AA))
val WIDGET_ON_ACCENT = ColorProvider(Color(0xFF0B1512))
val WIDGET_LOW = ColorProvider(Color(0xFFFF8A78))
val WIDGET_AWAY = ColorProvider(Color(0xFF5E6B67))
val WIDGET_TRACK = ColorProvider(Color(0x1FE8EFEC))

// Tile fills are see-through so the white value stays readable on top
val WIDGET_FILL = ColorProvider(Color(0x476FD1AA))
val WIDGET_FILL_LOW = ColorProvider(Color(0x47FF8A78))
val WIDGET_FILL_AWAY = ColorProvider(Color(0x335E6B67))

/** Rings dimmed while the buds are away, so last values never look live. */
val AWAY_RING_COLORS = DARK_RING_COLORS.copy(accent = 0xFF5E6B67.toInt(), low = 0xFF5E6B67.toInt(), glyph = 0xFF9AAAA5.toInt())

val WIDGET_PADDING = 12.dp
val WIDGET_GAP = 8.dp

val WIDGET_TITLE = TextStyle(color = WIDGET_TEXT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
val WIDGET_NOTE = TextStyle(color = WIDGET_MUTED, fontSize = 12.sp)

/**
 * The rounded dark card every widget sits on.
 *
 * @param modifier - extra modifier, such as a tap action for the whole widget
 * @param content - widget body
 */
@Composable
fun WidgetCard(modifier: GlanceModifier = GlanceModifier, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(WIDGET_CARD)
            .padding(WIDGET_PADDING),
    ) { content() }
}

/**
 * Space left inside the card for one side.
 *
 * @param side - widget side
 * @returns the side minus the card padding on both ends
 */
fun innerOf(side: Dp): Dp = side - WIDGET_PADDING * 2
