package com.anils.sarjmetre.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.anils.sarjmetre.ui.AppleType
import com.anils.sarjmetre.ui.LocalAppleColors

/** Space kept free under the content for the floating tab bar, so nothing rests beneath the glass. */
val TabBarClearance = 104.dp

/**
 * An iOS large-title page. The title scrolls away and reappears small and centered at the top,
 * and the scroll edge effect dissolves content where it slides under the status bar and the tab bar.
 */
@Composable
fun LargeTitlePage(
    title: String,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppleColors.current
    val density = LocalDensity.current
    val collapseStartPx = with(density) { 40.dp.toPx() }
    val collapseRangePx = with(density) { 16.dp.toPx() }
    val edgeRangePx = with(density) { 24.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 44.dp, bottom = TabBarClearance),
        ) {
            Text(title, style = AppleType.largeTitle, color = colors.label, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            content()
        }

        // Top scroll edge effect; it only appears once something has scrolled underneath.
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = (scrollState.value / edgeRangePx).coerceIn(0f, 1f) }
                .background(
                    // Solid enough under the inline title to keep it legible, then dissolving downward.
                    Brush.verticalGradient(
                        0f to colors.groupedBackground,
                        0.62f to colors.groupedBackground.copy(alpha = 0.97f),
                        1f to colors.groupedBackground.copy(alpha = 0f),
                    ),
                )
                .statusBarsPadding()
                .height(72.dp),
        )
        Text(
            title,
            style = AppleType.headline,
            color = colors.label,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .graphicsLayer { alpha = ((scrollState.value - collapseStartPx) / collapseRangePx).coerceIn(0f, 1f) },
        )

        // Bottom scroll edge effect under the floating tab bar.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to colors.groupedBackground.copy(alpha = 0f),
                        0.45f to colors.groupedBackground.copy(alpha = 0.7f),
                        1f to colors.groupedBackground.copy(alpha = 0.96f),
                    ),
                )
                .navigationBarsPadding()
                .height(96.dp),
        )
    }
}
