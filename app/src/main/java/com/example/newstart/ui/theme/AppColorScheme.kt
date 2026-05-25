package com.example.newstart.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
    val authGradient: List<Color>
)

object AppPalettes {
    val Blue = AppPalette(
        light = lightColorScheme(
            primary = Color(0xFF1D5FE2),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD9E2FF),
            onPrimaryContainer = Color(0xFF001945),
            background = Color(0xFFFDFBFF),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF1B1B1F),
            outline = Color(0xFF757780)
        ),
        dark = darkColorScheme(
            primary = Color(0xFF007AFF),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF003E80),
            onPrimaryContainer = Color(0xFFD1E4FF),
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            outline = Color(0xFF3A3A3C)
        ),
        authGradient = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1D5FE2),
            Color(0xFF1565C0)
        )
    )

    val RoyalGreen = AppPalette(
        light = lightColorScheme(
            primary = Color(0xFF004B23),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFBCF2C1),
            onPrimaryContainer = Color(0xFF00210E),
            background = Color(0xFFFDFBFF),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF191C19),
            outline = Color(0xFF717971)
        ),
        dark = darkColorScheme(
            primary = Color(0xFF00A34D), // Xanh lục bảo rực rỡ trên nền đen
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF00522B),
            onPrimaryContainer = Color(0xFFBCF2C1),
            background = Color(0xFF000000), // Chuẩn đen
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            outline = Color(0xFF3A3A3C)
        ),
        authGradient = listOf(
            Color(0xFF003217),
            Color(0xFF004B23),
            Color(0xFF00612D)
        )
    )

    val Red = AppPalette(
        light = lightColorScheme(
            primary = Color(0xFFB91D1D),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFEE2E2),
            onPrimaryContainer = Color(0xFF450A0A),
            background = Color(0xFFFDFBFF),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF201A1A),
            outline = Color(0xFF857372)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFFF3B30), // Đỏ rực rỡ chuẩn Apple/Vibrant
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF93000A),
            onPrimaryContainer = Color(0xFFFFDAD6),
            background = Color(0xFF000000), // Chuẩn đen
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            outline = Color(0xFF3A3A3C)
        ),
        authGradient = listOf(
            Color(0xFF7F1D1D),
            Color(0xFFB91D1D),
            Color(0xFFDC2626)
        )
    )

    fun getPalette(color: AppThemeColor): AppPalette = when (color) {
        AppThemeColor.BLUE -> Blue
        AppThemeColor.ROYAL_GREEN -> RoyalGreen
        AppThemeColor.RED -> Red
    }
}
