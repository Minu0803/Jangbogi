package com.minwoo.jangbogi.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF37658C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7EFF7),
    onPrimaryContainer = Color(0xFF284B6D),
    inversePrimary = Color(0xFFA6C8E8),
    secondary = Color(0xFF536B84),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4EBF2),
    onSecondaryContainer = Color(0xFF344B61),
    tertiary = Color(0xFF536C89),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE4EDF8),
    onTertiaryContainer = Color(0xFF29425F),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF1D2C3C),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF1D2C3C),
    surfaceVariant = Color(0xFFE4EBF2),
    onSurfaceVariant = Color(0xFF596A7C),
    surfaceTint = Color(0xFF37658C),
    inverseSurface = Color(0xFF293543),
    inverseOnSurface = Color(0xFFF2F6FB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF728397),
    outlineVariant = Color(0xFFDCE4ED),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDCE3EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5FA),
    surfaceContainer = Color(0xFFEBF0F6),
    surfaceContainerHigh = Color(0xFFE5ECF4),
    surfaceContainerHighest = Color(0xFFDEE7F1)
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFA6C8E8),
    onPrimary = Color(0xFF12334F),
    primaryContainer = Color(0xFF263D52),
    onPrimaryContainer = Color(0xFFD8E8F7),
    inversePrimary = Color(0xFF37658C),
    secondary = Color(0xFFB7C9DA),
    onSecondary = Color(0xFF24394C),
    secondaryContainer = Color(0xFF2C3D4D),
    onSecondaryContainer = Color(0xFFD9E5F0),
    tertiary = Color(0xFFB6CCE5),
    onTertiary = Color(0xFF21364D),
    tertiaryContainer = Color(0xFF374D66),
    onTertiaryContainer = Color(0xFFDCEAFB),
    background = Color(0xFF121922),
    onBackground = Color(0xFFE4EBF3),
    surface = Color(0xFF121922),
    onSurface = Color(0xFFE4EBF3),
    surfaceVariant = Color(0xFF344454),
    onSurfaceVariant = Color(0xFFB3C0CF),
    surfaceTint = Color(0xFFA6C8E8),
    inverseSurface = Color(0xFFE4EBF3),
    inverseOnSurface = Color(0xFF293543),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8799AD),
    outlineVariant = Color(0xFF344454),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF354352),
    surfaceDim = Color(0xFF101720),
    surfaceContainerLowest = Color(0xFF0D141C),
    surfaceContainerLow = Color(0xFF18222D),
    surfaceContainer = Color(0xFF1C2733),
    surfaceContainerHigh = Color(0xFF202C39),
    surfaceContainerHighest = Color(0xFF2A3847)
)

// 카드·항목 행·빠른 추가 바 공용 컨테이너 색. 라이트는 순백 카드, 다크는 띄워 보이는 톤이 필요해 모드별 슬롯이 다르다.
val ColorScheme.surfaceCard: Color
    get() = if (surface.luminance() > 0.5f) surfaceContainerLowest else surfaceContainerHigh


val ColorScheme.rouletteBuyContainer: Color
    get() = if (surface.luminance() > .5f) Color(0xFFDBEAF8) else Color(0xFF365C7C)
val ColorScheme.rouletteOnBuy: Color
    get() = if (surface.luminance() > .5f) Color(0xFF244B70) else Color(0xFFEDF5FC)
val ColorScheme.rouletteSkipContainer: Color
    get() = if (surface.luminance() > .5f) Color(0xFFE8EDF3) else Color(0xFF344454)
val ColorScheme.rouletteOnSkip: Color
    get() = if (surface.luminance() > .5f) Color(0xFF435365) else Color(0xFFE1E8F0)
