package com.anils.sarjmetre.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.LruCache
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a two-line status bar icon ("1850" over "mA"), like network speed meters do.
 * Only the alpha channel matters: the system tints notification icons itself.
 */
class StatusIconRenderer(private val sizePx: Int) {
    private val cache = LruCache<String, Bitmap>(32)
    private val valuePaint = textPaint()
    private val unitPaint = textPaint()
    private val bounds = Rect()

    fun render(value: String, unit: String): Bitmap {
        val key = "$value\n$unit"
        cache.get(key)?.let { return it }

        val size = sizePx.toFloat()
        val valueHeight = fit(valuePaint, value, "0", maxHeight = size * 0.56f, maxWidth = size)
        // Keep the unit visibly smaller than the number even when a long number had to shrink.
        val unitHeight = fit(unitPaint, unit, "A", maxHeight = min(size * 0.34f, valueHeight * 0.8f), maxWidth = size)
        val gap = size * 0.08f
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
        paint.getTextBounds(reference, 0, reference.length, bounds)
        paint.textSize = 100f * maxHeight / bounds.height()
        val width = paint.measureText(text)
        if (width > maxWidth) {
            paint.textScaleX = max(MIN_SCALE_X, maxWidth / width)
            val squeezed = paint.measureText(text)
            if (squeezed > maxWidth) paint.textSize *= maxWidth / squeezed
        }
        paint.getTextBounds(reference, 0, reference.length, bounds)
        return bounds.height().toFloat()
    }

    private fun textPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private companion object {
        /** Narrowest horizontal squeeze before the text gets smaller instead. */
        const val MIN_SCALE_X = 0.8f
    }
}
