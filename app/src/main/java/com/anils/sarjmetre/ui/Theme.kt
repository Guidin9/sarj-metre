package com.anils.sarjmetre.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anils.sarjmetre.R

/** Palette taken from the cell itself: cobalt (the cathode) for current flowing in, amber for current drawn out. */
@Immutable
data class MeterColors(
    val background: Color,
    val face: Color,
    val ink: Color,
    val inkMuted: Color,
    val hairline: Color,
    val outline: Color,
    val charge: Color,
    val chargeSoft: Color,
    val drain: Color,
    val drainText: Color,
    val drainSoft: Color,
)

private val LightColors = MeterColors(
    background = Color(0xFFEEF1F4),
    face = Color(0xFFFAFBFC),
    ink = Color(0xFF18202A),
    inkMuted = Color(0xFF5E6B78),
    hairline = Color(0xFFD5DBE2),
    outline = Color(0xFFAEB8C3),
    charge = Color(0xFF2350D1),
    chargeSoft = Color(0xFFDCE5FB),
    drain = Color(0xFFE08A00),
    drainText = Color(0xFF9A5B00),
    drainSoft = Color(0xFFF8E7CC),
)

private val DarkColors = MeterColors(
    background = Color(0xFF151E2A),
    face = Color(0xFF1C2734),
    ink = Color(0xFFE7EDF3),
    inkMuted = Color(0xFF93A1B0),
    hairline = Color(0xFF2C3947),
    outline = Color(0xFF46556A),
    charge = Color(0xFF86A8FF),
    chargeSoft = Color(0xFF24365C),
    drain = Color(0xFFFFB547),
    drainText = Color(0xFFFFB547),
    drainSoft = Color(0xFF3F321C),
)

val LocalMeterColors = staticCompositionLocalOf { LightColors }

/** Barlow: a DIN-lineage grotesk from road and rail signage, at home on an instrument face. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

/** The semi-condensed cut keeps long readings large. */
val BarlowSemiCondensed = FontFamily(
    Font(R.font.barlow_semicondensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semicondensed_semibold, FontWeight.SemiBold),
)

/** Figure styles on a classic 12-14-16-18-21-24-36-60 scale; tabular digits stop live numbers from jittering. */
object MeterType {
    val hero = TextStyle(
        fontFamily = BarlowSemiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )
    val heroUnit = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 21.sp)
    val figureLarge = TextStyle(
        fontFamily = BarlowSemiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontFeatureSettings = "tnum",
    )
    val figure = TextStyle(
        fontFamily = BarlowSemiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum",
    )
    val unit = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val dialNumber = TextStyle(
        fontFamily = BarlowSemiCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum",
    )
    val dialWord = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 13.sp)
}

private val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = BarlowSemiCondensed),
        displayMedium = displayMedium.copy(fontFamily = BarlowSemiCondensed),
        displaySmall = displaySmall.copy(fontFamily = BarlowSemiCondensed),
        headlineLarge = headlineLarge.copy(fontFamily = Barlow),
        headlineMedium = headlineMedium.copy(fontFamily = Barlow),
        headlineSmall = headlineSmall.copy(fontFamily = Barlow),
        titleLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = Barlow, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = Barlow, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = Barlow, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

private fun materialScheme(c: MeterColors, dark: Boolean): ColorScheme =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.charge,
        onPrimary = if (dark) Color(0xFF0B1B3D) else Color.White,
        primaryContainer = c.chargeSoft,
        onPrimaryContainer = c.ink,
        secondaryContainer = c.chargeSoft,
        onSecondaryContainer = c.ink,
        background = c.background,
        onBackground = c.ink,
        surface = c.background,
        onSurface = c.ink,
        surfaceVariant = c.face,
        onSurfaceVariant = c.inkMuted,
        surfaceContainerLowest = c.face,
        surfaceContainerLow = c.face,
        surfaceContainer = c.face,
        surfaceContainerHigh = c.face,
        surfaceContainerHighest = c.face,
        outline = c.outline,
        outlineVariant = c.hairline,
    )

@Composable
fun SarjMetreTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalMeterColors provides colors) {
        MaterialTheme(colorScheme = materialScheme(colors, dark), typography = AppTypography, content = content)
    }
}
