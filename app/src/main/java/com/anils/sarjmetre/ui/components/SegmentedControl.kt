package com.anils.sarjmetre.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.anils.sarjmetre.ui.AppleType
import com.anils.sarjmetre.ui.LocalAppleColors
import com.kyant.shapes.Capsule
import kotlin.math.roundToInt

/** The iOS 26 capsule segmented control: a sliding thumb on a quiet track. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppleColors.current
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val position by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.82f, stiffness = 520f), label = "thumb")

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(Capsule())
            .background(colors.fill)
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size
        val segmentPx = constraints.maxWidth / options.size.toFloat()
        Box(
            Modifier
                .offset { IntOffset((segmentPx * position).roundToInt(), 0) }
                .width(segment)
                .fillMaxHeight()
                .shadow(if (colors.isDark) 0.dp else 2.dp, Capsule(), clip = false, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(Capsule())
                .background(colors.segmentThumb),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (value, label) ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(Capsule())
                        .clickable(interactionSource = null, indication = null) { onSelect(value) }
                        .semantics {
                            role = Role.RadioButton
                            this.selected = i == index
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = AppleType.subheadline.copy(fontWeight = if (i == index) FontWeight.SemiBold else FontWeight.Medium),
                        color = colors.label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
