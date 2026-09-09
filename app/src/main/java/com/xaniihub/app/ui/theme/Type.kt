package com.xaniihub.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun xaniiTextStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
    tabular: Boolean = false
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    // Tabular figures keep animated counters from jittering horizontally.
    fontFeatureSettings = if (tabular) "tnum" else null
)

val XaniiTypography = Typography(
    displayLarge = xaniiTextStyle(FontWeight.Black, 64, 68, -2.0, tabular = true),
    displayMedium = xaniiTextStyle(FontWeight.ExtraBold, 48, 54, -1.5, tabular = true),
    displaySmall = xaniiTextStyle(FontWeight.Bold, 36, 42, -1.0, tabular = true),
    headlineLarge = xaniiTextStyle(FontWeight.Bold, 32, 38, -0.8),
    headlineMedium = xaniiTextStyle(FontWeight.Bold, 28, 34, -0.6),
    headlineSmall = xaniiTextStyle(FontWeight.SemiBold, 24, 30, -0.4),
    titleLarge = xaniiTextStyle(FontWeight.SemiBold, 22, 28, -0.3, tabular = true),
    titleMedium = xaniiTextStyle(FontWeight.SemiBold, 16, 22, -0.1),
    titleSmall = xaniiTextStyle(FontWeight.Medium, 14, 20),
    bodyLarge = xaniiTextStyle(FontWeight.Normal, 16, 24),
    bodyMedium = xaniiTextStyle(FontWeight.Normal, 14, 20),
    bodySmall = xaniiTextStyle(FontWeight.Normal, 12, 16),
    labelLarge = xaniiTextStyle(FontWeight.SemiBold, 14, 20, 0.2),
    labelMedium = xaniiTextStyle(FontWeight.SemiBold, 12, 16, 0.8),
    labelSmall = xaniiTextStyle(FontWeight.SemiBold, 10, 14, 1.4)
)