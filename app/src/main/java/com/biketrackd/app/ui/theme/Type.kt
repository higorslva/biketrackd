package com.biketrackd.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun scaledTypography(fontScale: Float): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = (96 * fontScale).sp,
        lineHeight = (96 * fontScale).sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = (48 * fontScale).sp,
        lineHeight = (52 * fontScale).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = (20 * fontScale).sp,
        lineHeight = (28 * fontScale).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = (16 * fontScale).sp,
        lineHeight = (24 * fontScale).sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = (14 * fontScale).sp,
        lineHeight = (20 * fontScale).sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = (14 * fontScale).sp,
        lineHeight = (20 * fontScale).sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (11 * fontScale).sp,
        lineHeight = (16 * fontScale).sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (14 * fontScale).sp,
        lineHeight = (20 * fontScale).sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (12 * fontScale).sp,
        lineHeight = (16 * fontScale).sp,
    ),
)
