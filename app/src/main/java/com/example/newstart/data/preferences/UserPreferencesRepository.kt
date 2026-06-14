package com.example.newstart.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val themeColorKey = stringPreferencesKey("theme_color")
    private val commonPomoTimesKey = stringPreferencesKey("common_pomo_times")
    private val isJournalPromptEnabledKey = booleanPreferencesKey("is_journal_prompt_enabled")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[themeModeKey] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val themeColorFlow: Flow<AppThemeColor> = context.dataStore.data.map { preferences ->
        val colorName = preferences[themeColorKey] ?: AppThemeColor.BLACK.name
        try {
            AppThemeColor.valueOf(colorName)
        } catch (e: Exception) {
            AppThemeColor.BLACK
        }
    }

    val commonPomoTimesFlow: Flow<List<Int>> = context.dataStore.data.map { preferences ->
        val timesString = preferences[commonPomoTimesKey] ?: "25,40,60,180"
        timesString.split(",").mapNotNull { it.toIntOrNull() }.sorted()
    }

    val isJournalPromptEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isJournalPromptEnabledKey] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setThemeColor(color: AppThemeColor) {
        context.dataStore.edit { preferences ->
            preferences[themeColorKey] = color.name
        }
    }

    suspend fun setCommonPomoTimes(times: List<Int>) {
        context.dataStore.edit { preferences ->
            preferences[commonPomoTimesKey] = times.joinToString(",")
        }
    }

    suspend fun setJournalPromptEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isJournalPromptEnabledKey] = enabled
        }
    }
}
