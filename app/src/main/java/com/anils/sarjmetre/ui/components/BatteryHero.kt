package com.anils.sarjmetre.ui.components

import android.os.BatteryManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.R
import com.anils.sarjmetre.ui.AppleType
import com.anils.sarjmetre.ui.LocalAppleColors
import com.anils.sarjmetre.ui.UiText

/**
 * The battery as the Batteries widget draws it: a ring for the charge level, green unless low,
 * with the live current at its center.
 */
@Composable
fun BatteryHero(state: MeterState?, running: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAppleColors.current
    val level = state?.level ?: 0
    val charging = state != null && state.isPlugged && state.status == BatteryManager.BATTERY_STATUS_CHARGING
    val ringColor = when {
        state == null -> colors.tertiaryLabel
        !charging && level <= 20 -> colors.red
        else -> colors.green
    }
    // The ring fills in once when the screen opens, then follows the level.
    val sweep by animateFloatAsState(level / 100f, tween(durationMillis = 900, easing = FastOutSlowInEasing), label = "ring")
    val current = state?.currentMa
    val description = buildString {
        append("Pil yüzde $level")
        if (current != null) append(", akım ${UiText.signedMa(current)} miliamper")
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(colors.cell)
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(236.dp)
                .clearAndSetSemantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 22.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(ringColor.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(R.drawable.ic_bolt_fill),
                    contentDescription = null,
                    tint = if (charging) colors.green else Color.Transparent,
                    modifier = Modifier.size(26.dp),
                )
                Row {
                    Text(
                        current?.let(UiText::signedMa) ?: "–",
                        style = AppleType.heroNumber,
                        color = colors.label,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(" mA", style = AppleType.title3, color = colors.secondaryLabel, modifier = Modifier.alignByBaseline())
                }
                Text(
                    if (state != null) "%$level" else " ",
                    style = AppleType.subheadline.merge(AppleType.figures).copy(fontWeight = FontWeight.SemiBold),
                    color = colors.secondaryLabel,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        val headline = state?.let { UiText.timeLine(it) ?: UiText.stateLine(it) } ?: if (running) "Ölçülüyor" else "Gösterge kapalı"
        val detail = state?.let { if (UiText.timeLine(it) != null) UiText.stateLine(it) else null }
        Text(headline, style = AppleType.headline, color = colors.label, textAlign = TextAlign.Center)
        if (detail != null) {
            Text(detail, style = AppleType.subheadline, color = colors.secondaryLabel, textAlign = TextAlign.Center)
        }
    }
}
