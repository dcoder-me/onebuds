package com.example.budsbattery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.budsbattery.data.NoiseMode

private const val GLYPH_SHARE = 0.46f
private const val BOLT_SIZE = 10f
private const val WAITING_SHARE = 0.3f
private const val FULL_PERCENT = 100f
private const val BADGE_CENTER = 33f
private const val BADGE_RADIUS = 7f
private const val BADGE_ICON = 10f
private const val ON_ACCENT = 0xFF0B1512.toInt()

/**
 * Draws the ring picture for the Live Update: the case level while a bud sits in it (else the lower bud), its picture
 * in the middle, a bolt when charging, else a badge with the active noise mode.
 *
 * @description A large icon is a plain picture, not a custom layout, so Android still promotes the notification.
 * @param context - app context, for size, theme and the drawables
 * @param view - what the screen shows
 * @param mode - active noise mode; null leaves out the badge
 * @returns a square bitmap at the system's large-icon size
 */
fun liveUpdateIcon(context: Context, view: BudsView, mode: NoiseMode?): Bitmap {
    val size = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    val colors = ringColorsFor(context)
    val spec = RingSpec(shareOf(view), view.low, shapeIcon(view.shape), GLYPH_SHARE)
    return ringBitmap(context, size, spec, colors) {
        when {
            view.focusCharging -> drawBolt(this)
            mode != null -> drawModeBadge(context, this, mode, colors.accent)
        }
    }
}

/**
 * Picks the notification accent colour, matching the ring.
 *
 * @param context - app context, for the theme
 * @param low - true when the lower bud is at or below the low level
 * @returns green, or red when low, as an ARGB int
 */
fun liveAccentColor(context: Context, low: Boolean): Int = ringColorsFor(context).let { if (low) it.low else it.accent }

// While nothing is read yet a short arc stands in, so the picture never looks empty
private fun shareOf(view: BudsView): Float {
    val lower = view.focus
    if (view.waiting || lower == null) return WAITING_SHARE
    return lower.level.percent / FULL_PERCENT
}

private fun drawModeBadge(context: Context, canvas: Canvas, mode: NoiseMode, accent: Int) {
    canvas.drawCircle(BADGE_CENTER, BADGE_CENTER, BADGE_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
    drawCentered(context, canvas, noiseIcon(mode), ON_ACCENT, BADGE_CENTER, BADGE_CENTER, BADGE_ICON)
}

private fun drawBolt(canvas: Canvas) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = BOLT_SIZE
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("⚡", RING_BOX, RING_BOX - paint.descent(), paint)
}
