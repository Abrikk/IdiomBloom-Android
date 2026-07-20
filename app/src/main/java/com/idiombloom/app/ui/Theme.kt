package com.idiombloom.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

val Ink = Color(0xFF20363D)
val SecondaryInk = Color(0xFF687A80)
val SuccessGreen = Color(0xFF32B879)
val ErrorRed = Color(0xFFFF5D68)
val Peach = Color(0xFFFFF1E8)
val Lavender = Color(0xFFF1EDFF)
val Cream = Color(0xFFFCFBF7)
val Gold = Color(0xFFF4B83F)

val AppBackground: Brush
    get() = Brush.linearGradient(
        colors = listOf(
            Peach.copy(alpha = 0.94f),
            Color(0xFFFFFBF8),
            Lavender.copy(alpha = 0.82f),
            Color(0xFFF4FAF7),
            Cream,
        ),
        start = Offset.Zero,
        end = Offset(1500f, 2300f),
    )

private val AppColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = SuccessGreen,
    error = ErrorRed,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3F4F2),
    onSurfaceVariant = SecondaryInk,
    outline = Ink.copy(alpha = 0.12f),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.3f).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 33.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun IdiomBloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
