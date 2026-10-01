package com.packpal.kids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PackPalColors {
    val Cream = Color(0xFFFFF8EC)
    val CreamDeep = Color(0xFFF6EBD6)
    val Teal = Color(0xFF5FA8A0)
    val TealDark = Color(0xFF3E7F79)
    val TealLight = Color(0xFFA9D6CF)
    val Navy = Color(0xFF1F2A44)
    val NavySoft = Color(0xFF4A5570)
    val Yellow = Color(0xFFF6C343)
    val YellowSoft = Color(0xFFFBE3A2)
    val Coral = Color(0xFFE38B7A)
    val CoralSoft = Color(0xFFF7D8D0)
    val White = Color(0xFFFFFFFF)

    // Compartment fabrics (light enough for navy text at AA contrast).
    val PocketBooks = Color(0xFF8FC8C0)
    val PocketFood = Color(0xFFB4DDD7)
    val PocketClothes = Color(0xFFC5E5E0)
    val PocketTools = Color(0xFF9CCFC8)
    val PocketImportant = Color(0xFFD8EFEB)
    val Stitch = Color(0xFFFFF8EC)
}

/** Parent-controlled preference: brief state-change animations on/off. */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

private val Scheme = lightColorScheme(
    primary = PackPalColors.Navy,
    onPrimary = PackPalColors.White,
    primaryContainer = PackPalColors.Yellow,
    onPrimaryContainer = PackPalColors.Navy,
    secondary = PackPalColors.TealDark,
    onSecondary = PackPalColors.White,
    secondaryContainer = PackPalColors.TealLight,
    onSecondaryContainer = PackPalColors.Navy,
    tertiary = PackPalColors.Coral,
    onTertiary = PackPalColors.Navy,
    background = PackPalColors.Cream,
    onBackground = PackPalColors.Navy,
    surface = PackPalColors.Cream,
    onSurface = PackPalColors.Navy,
    surfaceVariant = PackPalColors.CreamDeep,
    onSurfaceVariant = PackPalColors.NavySoft,
    surfaceContainer = PackPalColors.White,
    surfaceContainerHigh = PackPalColors.White,
    surfaceContainerHighest = PackPalColors.CreamDeep,
    surfaceContainerLow = PackPalColors.Cream,
    outline = PackPalColors.Navy,
    outlineVariant = PackPalColors.NavySoft,
    error = Color(0xFFB3261E),
)

private val Base = FontFamily.SansSerif

private val PackPalTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
)

@Composable
fun PackPalTheme(animationsEnabled: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAnimationsEnabled provides animationsEnabled) {
        MaterialTheme(colorScheme = Scheme, typography = PackPalTypography, content = content)
    }
}
