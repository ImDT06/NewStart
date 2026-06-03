package com.example.newstart.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository
) : ViewModel() {

    val userState: StateFlow<User?> = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val todayHabits: StateFlow<List<Habit>> = habitRepository
        .getHabits(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todos: StateFlow<List<Todo>> = todoRepository.getTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleHabit(habit: Habit, isCompleted: Boolean) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit, isCompleted)
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            todoRepository.toggleTodoCompletion(todoId, isCompleted)
        }
    }

    fun addTodo(task: String) {
        viewModelScope.launch {
            todoRepository.insertTodo(Todo(task = task))
        }
    }
}
