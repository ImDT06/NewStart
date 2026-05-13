package com.example.newstart.ui.util

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Custom Multipreview annotation that displays both Vietnamese and English locales.
 * This is the professional standard for ensuring UI scales correctly across languages.
 */
@Preview(name = "Vietnamese", group = "Locale", locale = "vi", showBackground = true)
@Preview(name = "English", group = "Locale", locale = "en", showBackground = true)
annotation class LanguagePreviews

/**
 * Combined Multipreview annotation for both Languages and Themes (Light/Dark).
 * Professionals use this for high-level UI components to verify all states at once.
 */
@LanguagePreviews
@Preview(name = "Dark Mode", group = "Theme", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class DevicePreviews