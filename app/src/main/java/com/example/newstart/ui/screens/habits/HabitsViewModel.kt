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
                val result = aiService.processCommand(command)
                handleAiResult(result)
                _aiState.value = AiState.Success("Đã thực hiện xong!")
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Lỗi không xác định"
                android.util.Log.e("HabitsViewModel", "AI Error: $errorMsg")
                _aiState.value = AiState.Error(errorMsg)
            }
        }
    }

    private suspend fun handleAiResult(json: JSONObject) {
        val action = json.optString("action")
        val data = json.optJSONObject("data") ?: return
        
        when (action) {
            "ADD" -> {
                val name = data.optString("name")
                val icon = data.optString("icon", "✨")
                val time = data.optString("time")
                val mins = data.optInt("minsBefore", 0)
                if (name.isNotEmpty()) {
                    val newHabit = Habit(
                        name = name,
                        icon = icon,
                        reminderTime = if (time.isNotEmpty()) time else null,
                        reminderMinutesBefore = mins,
                        date = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
                    repository.saveHabit(newHabit)
                }
            }
            "DELETE" -> {
                val nameToDelete = data.optString("name")
                habits.value.find { it.name.contains(nameToDelete, ignoreCase = true) }?.let {
                    repository.deleteHabit(it.id)
                }
            }
        }
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
