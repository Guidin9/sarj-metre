package com.anils.sarjmetre.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Scale of the center-zero ammeter: −1 is full discharge, +1 full charge. */
internal object DialScale {
    const val FULL_SCALE_MA = 6000f
    const val HALF_SWEEP_DEG = 70f

    /** Square-root spacing, so the few hundred mA of everyday use still swing the needle visibly. */
    fun position(ma: Float): Float {
        val t = sqrt((abs(ma) / FULL_SCALE_MA).coerceAtMost(1f))
        return if (ma < 0) -t else t
    }

    val labeled = listOf(500, 1000, 2000, 4000, 6000)

    /** One minor tick between neighbouring labels keeps an even rhythm instead of a comb near zero. */
    val minor = listOf(250, 750, 1500, 3000, 5000)
}

/**
 * A center-zero ammeter like the charge/discharge gauges of old car dashboards:
 * the needle swings right while the battery charges and left while it drains.
 */
@Composable
fun AmmeterDial(currentMa: Int?, sweep: Boolean, onSwept: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalMeterColors.current
    val measurer = rememberTextMeasurer(cacheSize = 24)
    val motion = remember { ValueAnimator.areAnimatorsEnabled() }
    val needle = remember { Animatable(0f) }
    val target by rememberUpdatedState(currentMa?.let { DialScale.position(it.toFloat()) } ?: 0f)

    LaunchedEffect(Unit) {
        // A single self-test sweep the first time the screen opens, like a gauge at ignition.
        if (sweep && motion) needle.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        onSwept()
        snapshotFlow { target }.collectLatest { value ->
            if (motion) {
                needle.animateTo(value, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow))
            } else {
                needle.snapTo(value)
            }
        }
    }

    val description = currentMa?.let { "Akım ${UiText.signedMa(it)} miliamper" } ?: "Akım ölçülmüyor"
    val numberStyle = MeterType.dialNumber.copy(color = colors.inkMuted)
    val chargeWord = MeterType.dialWord.copy(color = colors.charge)
    val drainWord = MeterType.dialWord.copy(color = colors.drainText)
    val unitWord = MeterType.dialWord.copy(color = colors.inkMuted)

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.85f)
            .semantics { contentDescription = description },
    ) {
        val labelGap = 16.dp.toPx()
        val top = 4.dp.toPx() + labelGap + 10.dp.toPx()
        // Room below the pivot for the needle's tail, clear of the reading underneath.
        val radius = size.height - top - 24.dp.toPx()
        val pivot = Offset(size.width / 2, top + radius)
        val halfSweepRad = Math.toRadians(DialScale.HALF_SWEEP_DEG.toDouble()).toFloat()
        val arcTopLeft = Offset(pivot.x - radius, pivot.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        fun pointAt(position: Float, distance: Float): Offset {
            val angle = position * halfSweepRad
            return Offset(pivot.x + distance * sin(angle), pivot.y - distance * cos(angle))
        }

        fun label(text: String, at: Offset, style: TextStyle) {
            val layout = measurer.measure(text, style)
            drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
        }

        drawArc(
            color = colors.outline,
            startAngle = -90f - DialScale.HALF_SWEEP_DEG,
            sweepAngle = DialScale.HALF_SWEEP_DEG * 2,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )

        // The band from zero to the needle carries the only color: cobalt in, amber out.
        val position = needle.value
        if (abs(position) > 0.005f) {
            drawArc(
                color = if (position > 0) colors.charge else colors.drain,
                startAngle = -90f,
                sweepAngle = position * DialScale.HALF_SWEEP_DEG,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        for (ma in DialScale.minor) {
            for (side in listOf(-1f, 1f)) {
                val t = side * DialScale.position(ma.toFloat())
                drawLine(colors.inkMuted, pointAt(t, radius), pointAt(t, radius - 6.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
            }
        }
        for (ma in DialScale.labeled) {
            for (side in listOf(-1f, 1f)) {
                val t = side * DialScale.position(ma.toFloat())
                drawLine(colors.ink, pointAt(t, radius), pointAt(t, radius - 11.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                label(UiText.amps(ma), pointAt(t, radius + labelGap), numberStyle)
            }
        }
        drawLine(colors.ink, pointAt(0f, radius + 2.dp.toPx()), pointAt(0f, radius - 14.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
        label("0", pointAt(0f, radius + labelGap), numberStyle)
        label("A", pointAt(0f, radius - 30.dp.toPx()), unitWord)
        label("Harcama", pointAt(-0.8f, radius - 38.dp.toPx()), drainWord)
        label("Şarj", pointAt(0.8f, radius - 38.dp.toPx()), chargeWord)

        // Tapered needle with a short counterweight tail behind the pivot.
        val angle = position * halfSweepRad
        val along = Offset(sin(angle), -cos(angle))
        val across = Offset(cos(angle), sin(angle))
        val tip = pivot + along * (radius - 9.dp.toPx())
        val tail = pivot - along * 12.dp.toPx()
        val baseHalf = 3.dp.toPx()
        val tipHalf = 0.9.dp.toPx()
        val needlePath = Path().apply {
            val a = tail + across * baseHalf
            val b = tip + across * tipHalf
            val c = tip - across * tipHalf
            val d = tail - across * baseHalf
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            lineTo(d.x, d.y)
            close()
        }
        drawPath(needlePath, colors.ink)
        drawCircle(colors.ink, radius = 9.dp.toPx(), center = pivot)
        drawCircle(colors.background, radius = 3.5.dp.toPx(), center = pivot)
    }
}
