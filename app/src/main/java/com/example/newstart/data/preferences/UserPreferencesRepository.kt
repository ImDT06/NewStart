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
    val isSearchableFlow: Flow<Boolean> = dataStore.data.map { it.isSearchable }
    val isHabitNotificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { it.isHabitNotificationsEnabled }
    val isCommunityNotificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { it.isCommunityNotificationsEnabled }

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

    suspend fun setSearchable(enabled: Boolean) {
        dataStore.updateData { it.copy(isSearchable = enabled) }
    }

    suspend fun setHabitNotificationsEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(isHabitNotificationsEnabled = enabled) }
    }

    suspend fun setCommunityNotificationsEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(isCommunityNotificationsEnabled = enabled) }
    }
}
