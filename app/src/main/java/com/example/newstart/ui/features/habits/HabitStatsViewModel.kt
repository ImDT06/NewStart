package com.example.newstart.ui.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.domain.model.Habit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HeatMapState(
    val habitData: Map<LocalDate, Int> = emptyMap(),
    val maxCompletions: Int = 1
)

@HiltViewModel
class HabitStatsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    val heatMapState: StateFlow<HeatMapState> = habitRepository.getAllHabits()
        .map { habits ->
            val data = habits.filter { it.isCompleted }
                .groupBy { LocalDate.parse(it.date) }
                .mapValues { it.value.size }
            
            HeatMapState(
                habitData = data,
                maxCompletions = data.values.maxOfOrNull { it } ?: 1
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HeatMapState()
        )

    val allHabits: StateFlow<List<Habit>> = habitRepository.getAllHabits()
        .map { habits ->
            habits.sortedByDescending { it.date }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleHabit(habit: Habit, completed: Boolean) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit, completed)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
        }
    }

    fun deleteHabits(habitIds: Set<String>) {
        viewModelScope.launch {
            habitIds.forEach { habitId ->
                habitRepository.deleteHabit(habitId)
            }
        }
    }
}
