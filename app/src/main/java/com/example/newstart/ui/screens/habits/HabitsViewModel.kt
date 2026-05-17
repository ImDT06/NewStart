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
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {

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
