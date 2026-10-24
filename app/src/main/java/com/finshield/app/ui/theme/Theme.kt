package com.finshield.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FinColors = lightColorScheme(
    primary = Navy,
    onPrimary = CardWhite,
    secondary = Emerald,
    onSecondary = CardWhite,
    tertiary = EmeraldButton,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = SegmentTrack,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = DividerColor,
    error = ErrorRed,
)

private val FinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FinShieldTheme(content: @Composable () -> Unit) {
    // Light-only by design: matches the reference screenshots.
    MaterialTheme(
        colorScheme = FinColors,
        typography = FinTypography,
        shapes = FinShapes,
        content = content,
    )
}
