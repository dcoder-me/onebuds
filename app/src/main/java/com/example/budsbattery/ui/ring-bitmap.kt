package com.example.budsbattery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation

// Drawn in a 40-unit box, then scaled, so the Live Update and every widget ring match the approved design
const val RING_BOX = 40f
private const val CENTER = RING_BOX / 2
private const val RING_RADIUS = 17f
private const val RING_STROKE = 4f
private const val ICON_VIEWPORT = 24
private const val START_ANGLE = -90f
private const val FULL_SWEEP = 360f

/** Ring colours: green arc, red when low, faint track, and the centre picture. */
data class RingColors(val accent: Int, val low: Int, val track: Int, val glyph: Int)

/** Colours for a light background. */
val LIGHT_RING_COLORS = RingColors(0xFF1F7A5C.toInt(), 0xFFC4402F.toInt(), 0x1A16201D, 0xFF16201D.toInt())

/** Colours for a dark background, as in the approved design. */
val DARK_RING_COLORS = RingColors(0xFF6FD1AA.toInt(), 0xFFFF8A78.toInt(), 0x1FE8EFEC, 0xFFE8EFEC.toInt())

/**
 * Picks the ring colours for the system theme.
 *
 * @param context - context whose configuration says light or dark
 * @returns dark colours in night mode, else light
 */
fun ringColorsFor(context: Context): RingColors =
    if (context.resources.configuration.isNightModeActive) DARK_RING_COLORS else LIGHT_RING_COLORS

/**
 * What one ring shows.
 *
 * @property share - filled part of the ring, 0 to 1
 * @property low - true draws the arc in the low colour
 * @property glyph - picture in the middle
 * @property glyphShare - picture side as a share of the ring
 */
data class RingSpec(val share: Float, val low: Boolean, @DrawableRes val glyph: Int, val glyphShare: Float)

/**
 * Draws a battery ring with its picture in the middle; the arc alone shows the level.
 *
 * @param context - context for the drawable
 * @param sizePx - bitmap side in pixels
 * @param spec - what the ring shows
 * @param colors - colours to use
 * @param extras - anything drawn on top in the 40-unit box, such as a badge
 * @returns a square bitmap
 */
fun ringBitmap(context: Context, sizePx: Int, spec: RingSpec, colors: RingColors, extras: Canvas.() -> Unit = {}): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    canvas.scale(sizePx / RING_BOX, sizePx / RING_BOX)
    drawArc(canvas, colors.track, FULL_SWEEP)
    if (spec.share > 0f) drawArc(canvas, if (spec.low) colors.low else colors.accent, FULL_SWEEP * spec.share.coerceAtMost(1f))
    drawCentered(context, canvas, spec.glyph, colors.glyph, CENTER, CENTER, RING_BOX * spec.glyphShare)
    canvas.extras()
    return bitmap
}

/**
 * Draws a tinted drawable centred on a point, scaled by the canvas so it never snaps to whole pixels.
 *
 * @param context - context for the drawable
 * @param canvas - canvas in 40-unit box space
 * @param icon - drawable with a 24-unit viewport
 * @param color - tint
 * @param x - centre x
 * @param y - centre y
 * @param side - drawn side
 */
fun drawCentered(context: Context, canvas: Canvas, @DrawableRes icon: Int, color: Int, x: Float, y: Float, side: Float) {
    val glyph = ContextCompat.getDrawable(context, icon)?.mutate() ?: return
    glyph.setTint(color)
    glyph.setBounds(0, 0, ICON_VIEWPORT, ICON_VIEWPORT)
    canvas.withTranslation(x - side / 2, y - side / 2) {
        withScale(side / ICON_VIEWPORT, side / ICON_VIEWPORT) { glyph.draw(this) }
    }
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
