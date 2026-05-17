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
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _selectedHabitDate = MutableStateFlow(LocalDate.now())
    val selectedHabitDate: StateFlow<LocalDate> = _selectedHabitDate.asStateFlow()

    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    fun onHabitDateSelected(date: LocalDate) {
        _selectedHabitDate.value = date
    }

    fun startEditingHabit(habit: Habit?) {
        _editingHabit.value = habit
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

    fun setAvatarUri(uri: Uri?) {
        val userId = currentUser.value?.id ?: return
        if (uri == null) return
        
        viewModelScope.launch {
            _isUploading.value = true
            val result = userRepository.updateAvatar(userId, uri)
            _isUploading.value = false
            if (result.isSuccess) {
                // Tự động cập nhật thông qua flow authRepository.currentUser
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
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
}
