package com.anils.sarjmetre.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.anils.sarjmetre.net.NetText
import com.anils.sarjmetre.net.UsageBar
import com.anils.sarjmetre.ui.AppleType
import com.anils.sarjmetre.ui.LocalAppleColors
import kotlin.math.max
import kotlin.math.min

/**
 * Stacked bars of data use, Wi-Fi at the base and mobile on top, the way Apple's Screen Time
 * draws its day. [slots] and [label] fix the axis (24 hours, 7 days, the month's days) even
 * before every bar has data. Tapping a bar selects it; tapping it again clears the selection.
 */
@Composable
fun UsageChart(
    bars: List<UsageBar>,
    slots: Int,
    labelEvery: Int,
    label: (Int) -> String?,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppleColors.current
    val measurer = rememberTextMeasurer(cacheSize = 40)
    val axisStyle = AppleType.caption2.merge(AppleType.figures).copy(color = colors.tertiaryLabel)
    val peak = bars.maxOfOrNull { it.totalBytes } ?: 0L
    val top = max(peak, 1L)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(170.dp)
            .semantics { contentDescription = "Kullanım grafiği, en yüksek ${NetText.bytes(peak)}" }
            .pointerInput(bars.size, slots, selected) {
                detectTapGestures { tap ->
                    val plotWidth = size.width - 52.dp.toPx()
                    val index = (tap.x / (plotWidth / slots)).toInt()
                    onSelect(if (index in bars.indices && index != selected) index else null)
                }
            },
    ) {
        val labelSpace = 52.dp.toPx()
        val axisHeight = 18.dp.toPx()
        val plotWidth = size.width - labelSpace
        val plotHeight = size.height - axisHeight
        val slotWidth = plotWidth / slots
        val barWidth = min(slotWidth * 0.62f, 18.dp.toPx())
        val gap = 2.dp.toPx()
        val radius = min(4.dp.toPx(), barWidth / 2)

        // Recessive grid: the peak and the baseline, labeled at the right like Apple's charts.
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(colors.separator, Offset(0f, 0f), Offset(plotWidth, 0f), 1.dp.toPx(), pathEffect = dash)
        drawLine(colors.separator, Offset(0f, plotHeight), Offset(plotWidth, plotHeight), 1.dp.toPx())
        val peakLabel = measurer.measure(if (peak > 0) NetText.bytes(peak) else "0", axisStyle)
        drawText(peakLabel, topLeft = Offset(plotWidth + 6.dp.toPx(), 0f))
        drawText(measurer.measure("0", axisStyle), topLeft = Offset(plotWidth + 6.dp.toPx(), plotHeight - peakLabel.size.height))

        bars.forEachIndexed { i, bar ->
            val x = i * slotWidth + (slotWidth - barWidth) / 2
            val dim = selected != null && selected != i
            val wifiHeight = bar.wifiBytes.toFloat() / top * plotHeight
            val mobileHeight = bar.mobileBytes.toFloat() / top * plotHeight
            val wifiColor = colors.blue.copy(alpha = if (dim) 0.35f else 1f)
            val mobileColor = colors.mobileData.copy(alpha = if (dim) 0.35f else 1f)
            // Only the topmost segment gets the rounded end; segments are separated by a surface gap.
            if (mobileHeight >= 0.5f && wifiHeight >= 0.5f) {
                drawSegment(wifiColor, x, plotHeight - wifiHeight, barWidth, wifiHeight, 0f)
                val mobileTop = plotHeight - wifiHeight - gap - mobileHeight
                drawSegment(mobileColor, x, mobileTop, barWidth, mobileHeight, radius)
            } else if (wifiHeight >= 0.5f) {
                drawSegment(wifiColor, x, plotHeight - wifiHeight, barWidth, wifiHeight, radius)
            } else if (mobileHeight >= 0.5f) {
                drawSegment(mobileColor, x, plotHeight - mobileHeight, barWidth, mobileHeight, radius)
            }
        }

        for (i in 0 until slots step labelEvery) {
            val text = label(i) ?: continue
            val layout = measurer.measure(text, axisStyle)
            val x = (i * slotWidth + slotWidth / 2 - layout.size.width / 2f).coerceIn(0f, plotWidth - layout.size.width)
            drawText(layout, topLeft = Offset(x, plotHeight + 4.dp.toPx()))
        }
    }
}

private fun DrawScope.drawSegment(color: Color, x: Float, y: Float, width: Float, height: Float, radius: Float) {
    val r = CornerRadius(min(radius, height), min(radius, height))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = x,
                top = y,
                right = x + width,
                bottom = y + height,
                topLeftCornerRadius = r,
                topRightCornerRadius = r,
                bottomRightCornerRadius = CornerRadius.Zero,
                bottomLeftCornerRadius = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}
