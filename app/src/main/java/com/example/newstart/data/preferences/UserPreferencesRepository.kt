package com.example.newstart.data.preferences

import androidx.datastore.core.DataStore
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>
) {
    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { it.themeMode }
    val themeColorFlow: Flow<AppThemeColor> = dataStore.data.map { it.themeColor }
    val commonPomoTimesFlow: Flow<List<Int>> = dataStore.data.map { it.commonPomoTimes }
    val isJournalPromptEnabledFlow: Flow<Boolean> = dataStore.data.map { it.isJournalPromptEnabled }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { it.copy(themeMode = mode) }
    }

    suspend fun setThemeColor(color: AppThemeColor) {
        dataStore.updateData { it.copy(themeColor = color) }
    }

    suspend fun setCommonPomoTimes(times: List<Int>) {
        dataStore.updateData { it.copy(commonPomoTimes = times) }
    }

    suspend fun setJournalPromptEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(isJournalPromptEnabled = enabled) }
    }
}
