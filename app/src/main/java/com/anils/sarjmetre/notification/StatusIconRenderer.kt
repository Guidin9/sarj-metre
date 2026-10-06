package com.anils.sarjmetre.notification

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.LruCache
import com.anils.sarjmetre.R
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a two-line status bar icon ("1850" over "mA"), like network speed meters do: a big number
 * over a small unit. Only the alpha channel matters: the system tints notification icons itself.
 *
 * Barlow Condensed has narrow figures, so a three or four digit number fits the square taller than
 * in Roboto Condensed, and its short comma stays off the unit line.
 */
class StatusIconRenderer(resources: Resources, private val sizePx: Int) {
    private val cache = LruCache<String, Bitmap>(32)
    private val valuePaint = textPaint(resources.getFont(R.font.barlow_condensed_bold)).apply {
        // Equal-width figures: a changing number keeps its size and place.
        fontFeatureSettings = "'tnum'"
    }
    private val unitPaint = textPaint(resources.getFont(R.font.barlow_condensed_semibold))
    private val bounds = Rect()

    fun render(value: String, unit: String): Bitmap {
        val key = "$value\n$unit"
        cache.get(key)?.let { return it }

        val size = sizePx.toFloat()
        var valueHeight = fit(valuePaint, value, "0", maxHeight = size * VALUE_SHARE, maxWidth = size)
        // Keep the unit visibly smaller than the number even when a long number had to shrink.
        val unitHeight = fit(unitPaint, unit, "A", maxHeight = min(size * UNIT_SHARE, valueHeight * 0.8f), maxWidth = size)
        var gap = gapBelow(value, size)
        val room = size * FILL - gap - unitHeight
        if (valueHeight > room) {
            // The comma's room made the block too tall, so the number gets a little smaller.
            valuePaint.textSize *= room / valueHeight
            valueHeight = glyphHeight(valuePaint, "0")
            gap = gapBelow(value, size)
        }
        val top = (size - valueHeight - gap - unitHeight) / 2

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawText(value, size / 2, top + valueHeight, valuePaint)
            drawText(unit, size / 2, top + valueHeight + gap + unitHeight, unitPaint)
        }
        cache.put(key, bitmap)
        return bitmap
    }

    /**
     * Sizes [paint] so the [reference] glyph is [maxHeight] tall, then squeezes and if needed
     * shrinks it until [text] fits [maxWidth]. Returns the resulting glyph height.
     */
    private fun fit(paint: Paint, text: String, reference: String, maxHeight: Float, maxWidth: Float): Float {
        paint.textScaleX = 1f
        paint.textSize = 100f
        paint.textSize = 100f * maxHeight / glyphHeight(paint, reference)
        val width = paint.measureText(text)
        if (width > maxWidth) {
            paint.textScaleX = max(MIN_SCALE_X, maxWidth / width)
            val squeezed = paint.measureText(text)
            if (squeezed > maxWidth) paint.textSize *= maxWidth / squeezed
        }
        return glyphHeight(paint, reference)
    }

    /** The gap under the number; a decimal comma hangs below the baseline and its tail must stay off the unit line. */
    private fun gapBelow(value: String, size: Float): Float {
        valuePaint.getTextBounds(value, 0, value.length, bounds)
        return max(size * GAP, bounds.bottom + size * COMMA_CLEARANCE)
    }

    private fun glyphHeight(paint: Paint, glyph: String): Float {
        paint.getTextBounds(glyph, 0, glyph.length, bounds)
        return bounds.height().toFloat()
    }

    private fun textPaint(font: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = font
    }

    private companion object {
        /** Narrowest horizontal squeeze before the text gets smaller instead. */
        const val MIN_SCALE_X = 0.8f

        /** Glyph heights as shares of the icon: the number's "0" and the unit's "A". */
        const val VALUE_SHARE = 0.61f
        const val UNIT_SHARE = 0.30f
        const val GAP = 0.07f

        /** Space left between a hanging comma and the unit line. */
        const val COMMA_CLEARANCE = 0.05f

        /** How much of the icon's height the two lines may take. */
        const val FILL = 0.98f
    }
}
