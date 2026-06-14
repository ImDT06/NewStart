package com.example.newstart.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.ui.MainViewModel

// Helper function to create the dynamic branded gradient for Auth screens
// It retrieves the custom handcrafted colors from the current palette
@Composable
fun authHeaderGradient(themeColor: AppThemeColor): Brush {
    val palette = AppPalettes.getPalette(themeColor)
    return remember(themeColor) {
        Brush.linearGradient(
            colors = palette.authGradient
        )
    }
}

// Overload for convenience if MainViewModel is available
@Composable
fun authHeaderGradient(mainViewModel: MainViewModel): Brush {
    val themeColor by mainViewModel.themeColor.collectAsStateWithLifecycle()
    return authHeaderGradient(themeColor)
}

val PrimaryBlue = Color(0xFF1D5FE2)
val RoyalGreenDeep = Color(0xFF004B23)
