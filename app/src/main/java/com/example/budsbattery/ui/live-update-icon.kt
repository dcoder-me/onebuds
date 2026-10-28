package com.example.budsbattery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.example.budsbattery.R

// Drawn in a 40-unit box, then scaled to the system's large-icon size
private const val BOX = 40f
private const val CENTER = BOX / 2
private const val RING_RADIUS = 17f
private const val RING_STROKE = 4f
private const val GLYPH_SHARE = 0.46f
private const val ICON_VIEWPORT = 24
private const val BOLT_SIZE = 10f
private const val START_ANGLE = -90f
private const val FULL_SWEEP = 360f
private const val WAITING_SHARE = 0.3f
private const val FULL_PERCENT = 100f

// Colours from the approved preview: green, red when low; lighter shades for a dark shade
private data class RingColors(val accent: Int, val low: Int, val track: Int, val glyph: Int)

private val LIGHT_COLORS = RingColors(0xFF1F7A5C.toInt(), 0xFFC4402F.toInt(), 0x1A16201D, 0xFF16201D.toInt())
private val DARK_COLORS = RingColors(0xFF6FD1AA.toInt(), 0xFFFF8A78.toInt(), 0x1FE8EFEC, 0xFFE8EFEC.toInt())

/**
 * Draws the ring picture for the Live Update: the lower bud's level, the buds in the middle, a bolt when charging.
 *
 * @description A large icon is a plain picture, not a custom layout, so Android still promotes the notification.
 * @param context - app context, for size, theme and the buds drawable
 * @param view - what the screen shows
 * @returns a square bitmap at the system's large-icon size
 */
fun liveUpdateIcon(context: Context, view: BudsView): Bitmap {
    val size = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    val colors = colorsFor(context)
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    canvas.scale(size / BOX, size / BOX)
    drawArc(canvas, colors.track, FULL_SWEEP)
    drawArc(canvas, if (view.low) colors.low else colors.accent, sweepOf(view))
    drawGlyph(context, canvas, view, colors.glyph)
    if (view.budsCharging) drawBolt(canvas)
    return bitmap
}

/**
 * Picks the notification accent colour, matching the ring.
 *
 * @param context - app context, for the theme
 * @param low - true when the lower bud is at or below the low level
 * @returns green, or red when low, as an ARGB int
 */
fun liveAccentColor(context: Context, low: Boolean): Int = colorsFor(context).let { if (low) it.low else it.accent }

private fun colorsFor(context: Context): RingColors =
    if (context.resources.configuration.isNightModeActive) DARK_COLORS else LIGHT_COLORS

// While nothing is read yet a short arc stands in, so the picture never looks empty
private fun sweepOf(view: BudsView): Float {
    val lower = view.lowerBud
    if (view.waiting || lower == null) return FULL_SWEEP * WAITING_SHARE
    return FULL_SWEEP * lower.level.percent / FULL_PERCENT
}

private fun drawArc(canvas: Canvas, color: Int, sweep: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = RING_STROKE
        strokeCap = Paint.Cap.ROUND
        this.color = color
    }
    val bounds = RectF(CENTER - RING_RADIUS, CENTER - RING_RADIUS, CENTER + RING_RADIUS, CENTER + RING_RADIUS)
    canvas.drawArc(bounds, START_ANGLE, sweep, false, paint)
}

private fun drawGlyph(context: Context, canvas: Canvas, view: BudsView, color: Int) {
    val icon = if (view.shape == BudsShape.ONE_BUD) R.drawable.ic_bud else R.drawable.ic_buds
    val glyph = ContextCompat.getDrawable(context, icon)?.mutate() ?: return
    val side = BOX * GLYPH_SHARE
    glyph.setTint(color)
    glyph.setBounds(0, 0, ICON_VIEWPORT, ICON_VIEWPORT)
    // Scaled by the canvas, not by integer bounds, so the glyph sits exactly in the middle
    canvas.withTranslation(CENTER - side / 2, CENTER - side / 2) {
        withScale(side / ICON_VIEWPORT, side / ICON_VIEWPORT) { glyph.draw(this) }
    }
}

private fun drawBolt(canvas: Canvas) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = BOLT_SIZE
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("⚡", BOX, BOX - paint.descent(), paint)
}
