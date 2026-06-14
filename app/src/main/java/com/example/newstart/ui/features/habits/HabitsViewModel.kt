package com.example.newstart.ui.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.data.remote.AiHabitService
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
import java.time.LocalDateTime
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
    private val habitRepository: HabitRepository,
    private val aiHabitService: AiHabitService
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

    fun restoreHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.saveHabit(habit)
        }
    }

    fun processAiCommand(command: String) {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                val response = aiHabitService.processCommand(command, currentTime)
                val results = response.getJSONArray("results")
                
                val draftHabits = mutableListOf<Habit>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val action = item.optString("action", "ADD")
                    if (action == "ADD") {
                        draftHabits.add(Habit(
                            id = "ai_${System.currentTimeMillis()}_$i",
                            name = item.optString("name", "Thói quen mới"),
                            icon = item.optString("icon", "✨"),
                            goal = "1",
                            colorHex = "#FF007AFF", 
                            reminderTime = item.optString("time", "08:00"),
                            reminderMinutesBefore = item.optInt("minsBefore", 5),
                            date = item.optString("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        ))
                    }
                }

                if (draftHabits.isNotEmpty()) {
                    _aiState.value = AiState.Drafting(draftHabits)
                } else {
                    _aiState.value = AiState.Error("Xin lỗi, tôi không tìm thấy thói quen nào trong câu lệnh của bạn.")
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitsViewModel", "AI Error: ${e.message}")
                _aiState.value = AiState.Error("Lỗi xử lý AI: ${e.localizedMessage}")
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
