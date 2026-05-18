package com.example.newstart.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.newstart.data.remote.AiHabitService
import org.json.JSONObject
import javax.inject.Inject

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Drafting(val habits: List<Habit>) : AiState()
    data class Success(val message: String) : AiState()
    data class Error(val message: String) : AiState()
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val aiService: AiHabitService
) : ViewModel() {

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState = _aiState.asStateFlow()

    fun processAiCommand(command: String) {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            try {
                val currentTime = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm"))
                val result = aiService.processCommand(command, currentTime)
                val drafts = parseAiResults(result)
                if (drafts.isEmpty()) {
                    _aiState.value = AiState.Error("Không tìm thấy thói quen nào trong câu lệnh.")
                } else {
                    _aiState.value = AiState.Drafting(drafts)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Lỗi không xác định"
                _aiState.value = AiState.Error(errorMsg)
            }
        }
    }

    private fun parseAiResults(json: JSONObject): List<Habit> {
        val results = json.optJSONArray("results") ?: return emptyList()
        val drafts = mutableListOf<Habit>()
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            val action = item.optString("action")
            if (action == "ADD") {
                drafts.add(Habit(
                    name = item.optString("name"),
                    icon = item.optString("icon", "✨"),
                    reminderTime = item.optString("time").ifEmpty { null },
                    reminderMinutesBefore = item.optInt("minsBefore", 5),
                    date = dateStr
                ))
            }
            // For DELETE, we might want to handle it differently in the draft flow or just execute it.
            // Professional apps usually ask to confirm additions, deletions are often direct or separate.
            // For now, let's focus on ADD for the draft UI.
        }
        return drafts
    }

    fun confirmAiHabits(habits: List<Habit>) {
        viewModelScope.launch {
            habits.forEach { repository.saveHabit(it) }
            _aiState.value = AiState.Success("Đã thêm ${habits.size} thói quen!")
        }
    }

    fun clearAiState() {
        _aiState.value = AiState.Idle
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<Habit>> = _selectedDate
        .flatMapLatest { date ->
            repository.getHabits(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addHabit(name: String, icon: String, goal: String, colorHex: String) {
        viewModelScope.launch {
            val newHabit = Habit(
                name = name,
                icon = icon,
                goal = goal,
                colorHex = colorHex,
                date = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            repository.saveHabit(newHabit)
        }
    }

    fun toggleHabit(habit: Habit, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habit, isCompleted)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }
}
