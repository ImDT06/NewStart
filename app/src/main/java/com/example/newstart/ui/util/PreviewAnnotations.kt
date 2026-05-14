package com.example.newstart.ui.util

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Professional Multipreview for Languages.
 */
@Preview(name = "Light - VI", group = "Locale", locale = "vi", showBackground = true)
@Preview(name = "Light - EN", group = "Locale", locale = "en", showBackground = true)
annotation class LanguagePreviews

/**
 * Professional Multipreview for Dark Mode.
 */
@Preview(name = "Dark - VI", group = "Theme", locale = "vi", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Dark - EN", group = "Theme", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class DarkModePreviews

/**
 * The "All-in-One" Preview: Checks Light/Dark and English/Vietnamese simultaneously.
 * Use this to ensure your UI is perfect in all common configurations.
 */
@LanguagePreviews
@DarkModePreviews
annotation class AppCombinedPreviews
