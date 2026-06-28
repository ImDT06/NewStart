package com.example.newstart.ui.features.home

import java.util.Date
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.Priority
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

    fun addTodo(task: String, priority: Priority = Priority.MEDIUM, dueDate: Date? = null) {
        viewModelScope.launch {
            todoRepository.insertTodo(Todo(task = task, priority = priority, dueDate = dueDate))
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.updateTodo(todo)
        }
    }

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        _timerSeconds.value = 25 * 60
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _timerSeconds.value -= 1
            }
            _isTimerRunning.value = false
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _timerSeconds.value = 0
    }
}
