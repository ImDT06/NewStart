package com.example.newstart.ui.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val message: String) : AiState()
    data class Error(val message: String) : AiState()
    data class Drafting(val habits: List<Habit>) : AiState()
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val _completedHabitForJournal = MutableStateFlow<Habit?>(null)
    val completedHabitForJournal: StateFlow<Habit?> = _completedHabitForJournal.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<Habit>> = _selectedDate
        .flatMapLatest { date ->
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            habitRepository.getHabits(dateStr)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleHabit(habit: Habit, completed: Boolean) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit, completed)
            if (completed) {
                _completedHabitForJournal.value = habit
            }
        }
    }

    fun clearJournalPrompt() {
        _completedHabitForJournal.value = null
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
        }
    }

    fun processAiCommand(command: String) {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                // Giả lập xử lý AI
                kotlinx.coroutines.delay(1500)
                
                if (command.contains("gym", ignoreCase = true)) {
                    val draft = Habit(
                        id = "temp_1",
                        name = "Tập Gym",
                        icon = "💪",
                        goal = "1",
                        colorHex = "#FF4285F4",
                        reminderTime = "17:00",
                        date = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
                    _aiState.value = AiState.Drafting(listOf(draft))
                } else {
                    _aiState.value = AiState.Error("Xin lỗi, tôi chưa hiểu yêu cầu này.")
                }
            } catch (e: Exception) {
                _aiState.value = AiState.Error("Đã có lỗi xảy ra: ${e.message}")
            }
        }
    }

    fun confirmAiHabits(habits: List<Habit>) {
        viewModelScope.launch {
            habits.forEach { habit ->
                habitRepository.saveHabit(habit)
            }
            _aiState.value = AiState.Success("Đã thêm thói quen thành công!")
        }
    }

    fun clearAiState() {
        _aiState.value = AiState.Idle
    }
}
