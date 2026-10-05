package com.anils.sarjmetre.notification

import kotlin.math.abs

/**
 * Decides when the notification is worth reposting, since every post makes SystemUI redraw the
 * status bar. The icon only follows the current once it has moved by [thresholdMa]; text-only
 * changes (voltage, temperature, times) wait up to [textEveryMs].
 */
class PostGate(private val thresholdMa: Int, private val textEveryMs: Long) {
    private var shownMa: Int? = null
    private var shownPlugged = false
    private var posted: MeterContent? = null
    private var postedAtMs = 0L

    /** The current the icon should show: the last shown one until [ma] moves far enough or flips sign. */
    fun iconMa(ma: Int?, plugged: Boolean): Int? {
        val shown = shownMa
        val hold = ma != null && shown != null && plugged == shownPlugged &&
            (ma < 0) == (shown < 0) && abs(ma - shown) < thresholdMa
        if (!hold) {
            shownMa = ma
            shownPlugged = plugged
        }
        return shownMa
    }

    /** Whether [content] should be posted now; if so it counts as posted. */
    fun shouldPost(content: MeterContent, nowMs: Long): Boolean {
        val last = posted
        val due = when {
            last == null -> true
            content.iconValue != last.iconValue || content.iconUnit != last.iconUnit || content.status != last.status -> true
            else -> content != last && nowMs - postedAtMs >= textEveryMs
        }
        if (due) {
            posted = content
            postedAtMs = nowMs
        }
        return due
    }

    /** Forget what was shown, e.g. after the static screen-off notification replaced it. */
    fun reset() {
        shownMa = null
        posted = null
    }
}
