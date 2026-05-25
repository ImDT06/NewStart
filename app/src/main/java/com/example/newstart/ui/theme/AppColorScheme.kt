package com.example.newstart.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
    val authGradient: List<Color> // Thêm bộ màu chuyên biệt cho Gradient
)

object AppPalettes {
    val Blue = AppPalette(
        light = lightColorScheme(
            primary = Color(0xFF1D5FE2),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD9E2FF),
            onPrimaryContainer = Color(0xFF001945),
            background = Color(0xFFFDFBFF), // Trắng sạch
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
            Color(0xFF0D47A1), // Màu Deep Blue gốc của bạn
            Color(0xFF1D5FE2), // Màu Primary
            Color(0xFF1565C0)  // Màu Medium Blue gốc của bạn
        )
    )

    val RoyalGreen = AppPalette(
        light = lightColorScheme(
            primary = Color(0xFF004B23),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFBCF2C1),
            onPrimaryContainer = Color(0xFF00210E),
            background = Color(0xFFFDFBFF), // Đưa về Trắng sạch giống Blue
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF191C19),
            outline = Color(0xFF717971)
        ),
        dark = darkColorScheme(
            primary = Color(0xFF80D99D),
            onPrimary = Color(0xFF00391A),
            primaryContainer = Color(0xFF00522B),
            onPrimaryContainer = Color(0xFFBCF2C1),
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFE1E3DF),
            outline = Color(0xFF8B938A)
        ),
        authGradient = listOf(
            Color(0xFF003217), // Deep Green (Đậm hơn 004B23)
            Color(0xFF004B23), // Royal Green Primary
            Color(0xFF00612D)  // Medium Green (Sáng hơn một chút)
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
            primary = Color(0xFFFFB4AB),
            onPrimary = Color(0xFF690005),
            primaryContainer = Color(0xFF93000A),
            onPrimaryContainer = Color(0xFFFFDAD6),
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFEDE0DF),
            outline = Color(0xFFA08C8B)
        ),
        authGradient = listOf(
            Color(0xFF7F1D1D), // Deep Red
            Color(0xFFB91D1D), // Crimson Primary
            Color(0xFFDC2626)  // Vibrant Red
        )
    )

    fun getPalette(color: AppThemeColor): AppPalette = when (color) {
        AppThemeColor.BLUE -> Blue
        AppThemeColor.ROYAL_GREEN -> RoyalGreen
        AppThemeColor.RED -> Red
    }
}
