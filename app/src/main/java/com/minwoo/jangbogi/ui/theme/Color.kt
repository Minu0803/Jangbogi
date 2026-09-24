package com.minwoo.jangbogi.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F6D40),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F5C8),
    onPrimaryContainer = Color(0xFF00210F),
    inversePrimary = Color(0xFF86D9A9),
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D8),
    onSecondaryContainer = Color(0xFF0A1F12),
    tertiary = Color(0xFF8F4C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF171D18),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDDE5DC),
    onSurfaceVariant = Color(0xFF414942),
    surfaceTint = Color(0xFF0F6D40),
    inverseSurface = Color(0xFF2C322D),
    inverseOnSurface = Color(0xFFEDF2EB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1C9C0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6FBF4),
    surfaceDim = Color(0xFFD6DCD5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEAF0E8),
    surfaceContainerHigh = Color(0xFFE4EBE3),
    surfaceContainerHighest = Color(0xFFDFE5DD)
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF86D9A9),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF00522F),
    onPrimaryContainer = Color(0xFFA8F5C8),
    inversePrimary = Color(0xFF0F6D40),
    secondary = Color(0xFFB5CCBB),
    onSecondary = Color(0xFF213529),
    secondaryContainer = Color(0xFF374B3E),
    onSecondaryContainer = Color(0xFFD0E8D8),
    tertiary = Color(0xFFFFB877),
    onTertiary = Color(0xFF4B2800),
    tertiaryContainer = Color(0xFF6B3B00),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDFE5DD),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDFE5DD),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9C0),
    surfaceTint = Color(0xFF86D9A9),
    inverseSurface = Color(0xFFDFE5DD),
    inverseOnSurface = Color(0xFF2C322D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414942),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353B36),
    surfaceDim = Color(0xFF0F1511),
    surfaceContainerLowest = Color(0xFF0A0F0B),
    surfaceContainerLow = Color(0xFF171D18),
    surfaceContainer = Color(0xFF1B211C),
    surfaceContainerHigh = Color(0xFF262C26),
    surfaceContainerHighest = Color(0xFF303631)
)

// 카드·항목 행·빠른 추가 바 공용 컨테이너 색. 라이트는 순백 카드, 다크는 띄워 보이는 톤이 필요해 모드별 슬롯이 다르다.
val ColorScheme.surfaceCard: Color
    get() = if (surface.luminance() > 0.5f) surfaceContainerLowest else surfaceContainerHigh
