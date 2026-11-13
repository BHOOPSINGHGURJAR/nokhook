package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NokhookDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = VioletContainer,
    onPrimaryContainer = VioletOnContainer,
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF042218),
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldOnContainer,
    tertiary = SelectGold,
    onTertiary = Color(0xFF2B1800),
    tertiaryContainer = Color(0xFF33220A),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = ObsidianBg,
    onBackground = CrispWhite,
    surface = ObsidianSurface,
    onSurface = CrispWhite,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = CoolGray,
    outline = ObsidianBorder,
    error = Color(0xFFF87171),
    onError = ObsidianBg
)

private val NokhookLightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = ParchmentLight,
    onBackground = InkPrimary,
    surface = IvorySurface,
    onSurface = InkPrimary,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = WarmSlate,
    outline = WarmBorder,
    error = TierSkip,
    onError = Color.White
)

val NokhookShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun NokhookTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NokhookDarkColorScheme else NokhookLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NokhookTypography,
        shapes = NokhookShapes,
        content = content
    )
}
