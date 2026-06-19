package com.example.newstart.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.data.preferences.UserPreferencesRepository
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.domain.repository.JournalRepository
import com.example.newstart.domain.repository.UserRepository
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class AuthState {
    Loading, Authenticated, Unauthenticated
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val journalRepository: JournalRepository,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
    private val database: com.example.newstart.data.local.NewStartDatabase
) : ViewModel() {

    private val _selectedHabitDate = MutableStateFlow(LocalDate.now())
    val selectedHabitDate: StateFlow<LocalDate> = _selectedHabitDate.asStateFlow()

    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    private val _showJournalSheet = MutableStateFlow(false)
    val showJournalSheet: StateFlow<Boolean> = _showJournalSheet.asStateFlow()

    fun onHabitDateSelected(date: LocalDate) {
        _selectedHabitDate.value = date
    }

    fun startEditingHabit(habit: Habit?) {
        _editingHabit.value = habit
    }

    fun setShowJournalSheet(show: Boolean) {
        _showJournalSheet.value = show
    }

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    // Lấy thông tin user từ Firestore dựa trên ID của Auth
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = authRepository.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) flowOf(null)
            else userRepository.getUserById(firebaseUser.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val authState: StateFlow<AuthState> = authRepository.currentUser
        .map { user ->
            if (user != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val themeColor: StateFlow<AppThemeColor> = userPreferencesRepository.themeColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeColor.BLACK
        )

    val commonPomoTimes: StateFlow<List<Int>> = userPreferencesRepository.commonPomoTimesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(25, 40, 60, 180)
        )

    val isJournalPromptEnabled: StateFlow<Boolean> = userPreferencesRepository.isJournalPromptEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val avatarUri: StateFlow<Uri?> = currentUser
        .map { user -> 
            user?.avatarUrl?.let { Uri.parse(it) } 
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: AppThemeColor) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeColor(color)
        }
    }

    fun setJournalPromptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setJournalPromptEnabled(enabled)
        }
    }

    fun addCommonPomoTime(minutes: Int) {
        viewModelScope.launch {
            val currentTimes = commonPomoTimes.value.toMutableList()
            if (!currentTimes.contains(minutes)) {
                currentTimes.add(minutes)
                userPreferencesRepository.setCommonPomoTimes(currentTimes.sorted())
            }
        }
    }

    fun removeCommonPomoTime(minutes: Int) {
        viewModelScope.launch {
            val currentTimes = commonPomoTimes.value.toMutableList()
            if (currentTimes.contains(minutes)) {
                currentTimes.remove(minutes)
                userPreferencesRepository.setCommonPomoTimes(currentTimes.sorted())
            }
        }
    }

    fun updateCommonPomoTime(oldMinutes: Int, newMinutes: Int) {
        viewModelScope.launch {
            val currentTimes = commonPomoTimes.value.toMutableList()
            if (currentTimes.contains(oldMinutes)) {
                currentTimes.remove(oldMinutes)
                if (!currentTimes.contains(newMinutes)) {
                    currentTimes.add(newMinutes)
                }
                userPreferencesRepository.setCommonPomoTimes(currentTimes.sorted())
            }
        }
    }

    fun setAvatarUri(uri: Uri?) {
        val userId = currentUser.value?.id ?: return
        if (uri == null) return
        
        viewModelScope.launch {
            _isUploading.value = true
            userRepository.updateAvatar(userId, uri)
            _isUploading.value = false
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                // 1. Clear database
                database.clearAllTables()
                
                // 2. Clear Auth
                authRepository.logout()
                
                // Lưu ý: Navigation được xử lý tự động trong MainActivity thông qua authState
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Lỗi khi logout: ${e.message}")
            }
        }
    }

    fun saveJournalEntry(emoji: String, text: String, imageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = journalRepository.saveJournalEntry(emoji, text, imageUri)
            _isUploading.value = false
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun saveHabit(
        id: String = "",
        name: String, 
        icon: String, 
        goal: String, 
        colorHex: String, 
        reminderTime: String? = null,
        reminderMinutesBefore: Int = 0,
        date: LocalDate? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val finalDate = date ?: _selectedHabitDate.value
            val newHabit = Habit(
                id = id,
                name = name,
                icon = icon,
                goal = goal,
                colorHex = colorHex,
                date = finalDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                reminderTime = reminderTime,
                reminderMinutesBefore = reminderMinutesBefore
            )
            val result = habitRepository.saveHabit(newHabit)
            if (result.isSuccess) {
                onSuccess()
            } else {
                // In lỗi ra Logcat để kiểm tra
                android.util.Log.e("MainViewModel", "Lưu thói quen thất bại: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private val _timerSeconds = MutableStateFlow(25 * 60)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _focusTime = MutableStateFlow(25)
    val focusTime = _focusTime.asStateFlow()

    private val _breakTime = MutableStateFlow(5)
    val breakTime = _breakTime.asStateFlow()

    private val _isFocusMode = MutableStateFlow(true)
    val isFocusMode = _isFocusMode.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun setFocusTime(minutes: Int) {
        _focusTime.value = minutes
        if (!_isTimerRunning.value && _isFocusMode.value) {
            _timerSeconds.value = minutes * 60
        }
    }

    fun setBreakTime(minutes: Int) {
        _breakTime.value = minutes
        if (!_isTimerRunning.value && !_isFocusMode.value) {
            _timerSeconds.value = minutes * 60
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        
        if (_timerSeconds.value <= 0) {
            _timerSeconds.value = (if (_isFocusMode.value) _focusTime.value else _breakTime.value) * 60
        }
        
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            try {
                while (_timerSeconds.value > 0) {
                    kotlinx.coroutines.delay(1000)
                    _timerSeconds.value -= 1
                }
                onTimerFinished()
            } catch (e: Exception) {
                // Timer cancelled
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerSeconds.value = (if (_isFocusMode.value) _focusTime.value else _breakTime.value) * 60
    }

    private fun onTimerFinished() {
        _isFocusMode.value = !_isFocusMode.value
        resetTimer()
    }

    fun stopTimer() {
        resetTimer()
    }

    // --- Thống kê người dùng (Real-time Stats) ---

    val journalCount: StateFlow<Int> = journalRepository.getJournalEntries()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val habitStats: StateFlow<Pair<Int, Int>> = habitRepository.getAllHabits()
        .map { habits ->
            if (habits.isEmpty()) 0 to 0
            else {
                val total = habits.size
                val completed = habits.count { it.isCompleted }
                val percent = (completed.toFloat() / total * 100).toInt()
                
                // Tính toán chuỗi ngày (Streak)
                val completedDates = habits.filter { it.isCompleted }
                    .map { it.date }
                    .distinct()
                    .map { LocalDate.parse(it) }
                    .sortedDescending()

                var streak = 0
                if (completedDates.isNotEmpty()) {
                    var current = LocalDate.now()
                    // Nếu hôm nay chưa hoàn thành gì, kiểm tra từ hôm qua
                    if (!completedDates.contains(current)) {
                        current = current.minusDays(1)
                    }
                    
                    for (date in completedDates) {
                        if (date == current) {
                            streak++
                            current = current.minusDays(1)
                        } else if (date.isBefore(current)) {
                            break
                        }
                    }
                }
                percent to streak
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0 to 0
        )
}
