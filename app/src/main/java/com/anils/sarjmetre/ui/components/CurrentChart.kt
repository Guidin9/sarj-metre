package com.anils.sarjmetre.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.ui.AppleType
import com.anils.sarjmetre.ui.LocalAppleColors
import com.anils.sarjmetre.ui.UiText
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** The recent current as an Apple-style line chart: green above zero (charging), orange below (draining). */
@Composable
fun CurrentChart(samples: List<Int>, intervalMs: Long, modifier: Modifier = Modifier) {
    val colors = LocalAppleColors.current
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val axisStyle = AppleType.caption2.merge(AppleType.figures).copy(color = colors.tertiaryLabel)

    Column(modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)) {
        if (samples.size < 2) {
            Text(
                "Grafik, ekran açıkken birkaç saniye içinde dolmaya başlar.",
                style = AppleType.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier
                    .height(140.dp)
                    .wrapContentHeight(),
            )
            return@Column
        }
        // Round the scale to whole hundreds so the axis labels read cleanly.
        val top = (ceil(max(samples.max(), 0) / 100.0) * 100).toInt()
        val bottom = (floor(min(samples.min(), 0) / 100.0) * 100).toInt()
        val range = max(top - bottom, 100)

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = "Son akım grafiği, en yüksek $top, en düşük $bottom miliamper" },
        ) {
            val labelSpace = 44.dp.toPx()
            val plotWidth = size.width - labelSpace
            val h = size.height
            fun y(value: Int) = (top - value).toFloat() / range * h
            val step = plotWidth / (LiveState.HISTORY_SIZE - 1)
            val startX = plotWidth - (samples.size - 1) * step
            val zeroY = y(0)

            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            for (value in setOf(top, 0, bottom)) {
                val lineY = y(value)
                drawLine(colors.separator, Offset(0f, lineY), Offset(plotWidth, lineY), 1.dp.toPx(), pathEffect = if (value == 0) null else dash)
                val layout = measurer.measure(if (value == 0) "0" else UiText.signedMa(value), axisStyle)
                val labelY = (lineY - layout.size.height / 2f).coerceIn(0f, h - layout.size.height)
                drawText(layout, topLeft = Offset(plotWidth + 6.dp.toPx(), labelY))
            }

            val line = Path()
            samples.forEachIndexed { i, value ->
                val x = startX + i * step
                if (i == 0) line.moveTo(x, y(value)) else line.lineTo(x, y(value))
            }
            val area = Path().apply {
                addPath(line)
                lineTo(plotWidth, zeroY)
                lineTo(startX, zeroY)
                close()
            }
            val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            clipRect(right = plotWidth, bottom = zeroY) {
                drawPath(area, Brush.verticalGradient(listOf(colors.green.copy(alpha = 0.32f), colors.green.copy(alpha = 0.04f)), startY = 0f, endY = zeroY))
                drawPath(line, colors.green, style = stroke)
            }
            clipRect(right = plotWidth, top = zeroY) {
                drawPath(area, Brush.verticalGradient(listOf(colors.orange.copy(alpha = 0.04f), colors.orange.copy(alpha = 0.32f)), startY = zeroY, endY = h))
                drawPath(line, colors.orange, style = stroke)
            }
            val last = samples.last()
            drawCircle(if (last >= 0) colors.green else colors.orange, 4.5.dp.toPx(), Offset(plotWidth, y(last)))
            drawCircle(colors.cell, 2.dp.toPx(), Offset(plotWidth, y(last)))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(end = 44.dp)) {
            val spanSeconds = (samples.size - 1) * intervalMs / 1000
            val span = if (spanSeconds >= 60) "${spanSeconds / 60} dk önce" else "$spanSeconds sn önce"
            Text(span, style = axisStyle, modifier = Modifier.weight(1f))
            Text("Şimdi", style = axisStyle)
        }
    }
}
