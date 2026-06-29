package com.example.newstart.data.preferences

import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: AppThemeColor = AppThemeColor.BLACK,
    val commonPomoTimes: List<Int> = listOf(25, 40, 60, 180),
    val isJournalPromptEnabled: Boolean = true,
    val isSearchable: Boolean = true
)
