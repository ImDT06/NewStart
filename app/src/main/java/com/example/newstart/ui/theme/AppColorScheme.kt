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
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1B1B1F),
            onSurfaceVariant = Color(0xFF44474E),
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
            onSurfaceVariant = Color(0xFFC4C6D0),
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
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191C19),
            onSurfaceVariant = Color(0xFF404943),
            outline = Color(0xFF717971)
        ),
        dark = darkColorScheme(
            primary = Color(0xFF00A34D), 
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF00522B),
            onPrimaryContainer = Color(0xFFBCF2C1),
            background = Color(0xFF000000), 
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFC0C9C1),
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
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF201A1A),
            onSurfaceVariant = Color(0xFF534341),
            outline = Color(0xFF857372)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFFF3B30), 
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF93000A),
            onPrimaryContainer = Color(0xFFFFDAD6),
            background = Color(0xFF000000), 
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFD8C2BF),
            outline = Color(0xFF3A3A3C)
        ),
        authGradient = listOf(
            Color(0xFF7F1D1D),
            Color(0xFFB91D1D),
            Color(0xFFDC2626)
        )
    )

    val Black = AppPalette(
        light = lightColorScheme(
            primary = Color(0xFF1B1B1F),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE3E2E6),
            onPrimaryContainer = Color(0xFF1B1B1F),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1B1B1F),
            onSurfaceVariant = Color(0xFF44474E),
            outline = Color(0xFF757780)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF2C2C2E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFC4C6D0),
            outline = Color(0xFF3A3A3C)
        ),
        authGradient = listOf(
            Color(0xFF000000),
            Color(0xFF1C1C1E),
            Color(0xFF2C2C2E)
        )
    )

    fun getPalette(color: AppThemeColor): AppPalette = when (color) {
        AppThemeColor.BLUE -> Blue
        AppThemeColor.ROYAL_GREEN -> RoyalGreen
        AppThemeColor.RED -> Red
        AppThemeColor.BLACK -> Black
    }
}
