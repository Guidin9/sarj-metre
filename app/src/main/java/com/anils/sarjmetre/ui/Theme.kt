package com.anils.sarjmetre.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anils.sarjmetre.R

/** iOS semantic system colors, so the app reads like one of Apple's own. */
@Immutable
data class AppleColors(
    val isDark: Boolean,
    val groupedBackground: Color,
    val cell: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val fill: Color,
    val segmentThumb: Color,
    val glassSurface: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val blue: Color,
    /** Mobile data in the usage chart, next to [blue] for Wi-Fi; darker in dark mode so both stay distinct for color-blind eyes. */
    val mobileData: Color,
)

private val LightApple = AppleColors(
    isDark = false,
    groupedBackground = Color(0xFFF2F2F7),
    cell = Color(0xFFFFFFFF),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43),
    fill = Color(0x1F767680),
    segmentThumb = Color(0xFFFFFFFF),
    glassSurface = Color(0x66FAFAFA),
    green = Color(0xFF34C759),
    orange = Color(0xFFFF9500),
    red = Color(0xFFFF3B30),
    blue = Color(0xFF007AFF),
    mobileData = Color(0xFF34C759),
)

private val DarkApple = AppleColors(
    isDark = true,
    groupedBackground = Color(0xFF000000),
    cell = Color(0xFF1C1C1E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    separator = Color(0x99545458),
    fill = Color(0x3D767680),
    segmentThumb = Color(0xFF636366),
    glassSurface = Color(0x66121212),
    green = Color(0xFF30D158),
    orange = Color(0xFFFF9F0A),
    red = Color(0xFFFF453A),
    blue = Color(0xFF0A84FF),
    mobileData = Color(0xFF24A846),
)

val LocalAppleColors = staticCompositionLocalOf { LightApple }

// SF Pro is licensed for Apple platforms only; Inter is the closest open equivalent.
// Its optical-size axis gives a text cut for body sizes and a display cut for large titles, like SF Text/Display.
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int, opticalSize: Float) = Font(
    R.font.inter,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

val InterText = FontFamily(inter(400, 16f), inter(500, 16f), inter(600, 16f), inter(700, 16f))
val InterDisplay = FontFamily(inter(400, 32f), inter(500, 32f), inter(600, 32f), inter(700, 32f))

/** The iOS Dynamic Type default sizes, with Inter's tracking pulled in to sit closer to SF. */
object AppleType {
    val largeTitle = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.6).sp)
    val title3 = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.3).sp)
    val headline = TextStyle(fontFamily = InterText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.3).sp)
    val body = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.3).sp)
    val subheadline = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp)
    val footnote = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp)
    val caption2 = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp)
    val tabLabel = TextStyle(fontFamily = InterText, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 12.sp)

    /** The live reading in the battery ring; tabular digits keep it from jittering every second. */
    val heroNumber = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum",
    )
    val figures = TextStyle(fontFeatureSettings = "tnum")
}

@Composable
fun SarjMetreTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkApple else LightApple
    // Material components (text field cursor, ripples) only pick up the tint and surfaces.
    val scheme = (if (colors.isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = colors.green,
        background = colors.groupedBackground,
        surface = colors.cell,
        onSurface = colors.label,
        onBackground = colors.label,
    )
    CompositionLocalProvider(LocalAppleColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
