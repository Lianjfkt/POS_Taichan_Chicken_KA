package com.kapos.mobile.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppFont = FontFamily.SansSerif

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = AppFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = AppFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontFamily = AppFont,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = AppFont,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary
    )
)
