package com.example.newstart.ui.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.model.Priority
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.domain.repository.TodoRepository
import com.example.newstart.data.remote.AiHabitService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val message: String) : AiState()
    data class Error(val message: String) : AiState()
    data class Drafting(val habits: List<Habit>, val todos: List<Todo>) : AiState()
}

enum class HabitFilter {
    ALL, COMPLETED, UNCOMPLETED
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository,
    private val aiHabitService: AiHabitService
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _filter = MutableStateFlow(HabitFilter.ALL)
    val filter: StateFlow<HabitFilter> = _filter.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val _completedHabitForJournal = MutableStateFlow<Habit?>(null)
    val completedHabitForJournal: StateFlow<Habit?> = _completedHabitForJournal.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<Habit>> = combine(_selectedDate, _filter) { date, filter ->
        date to filter
    }.flatMapLatest { (date, filter) ->
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        habitRepository.getHabits(dateStr).map { list ->
            when (filter) {
                HabitFilter.ALL -> list
                HabitFilter.COMPLETED -> list.filter { it.isCompleted }
                HabitFilter.UNCOMPLETED -> list.filter { !it.isCompleted }
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setFilter(filter: HabitFilter) {
        _filter.value = filter
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
                val draftTodos = mutableListOf<Todo>()
                
                // Lấy danh sách việc cần làm hiện tại để so khớp nếu cần dời lịch
                val currentTodos = todoRepository.getTodos().firstOrNull() ?: emptyList<Todo>()
                
                val isTodoCommand = command.contains("todo", ignoreCase = true) ||
                        command.contains("việc", ignoreCase = true) ||
                        command.contains("task", ignoreCase = true) ||
                        command.contains("cần làm", ignoreCase = true) ||
                        command.contains("deadline", ignoreCase = true) ||
                        command.contains("hạn", ignoreCase = true) ||
                        command.contains("báo cáo", ignoreCase = true) ||
                        command.contains("nộp", ignoreCase = true) ||
                        command.contains("mua", ignoreCase = true) ||
                        command.contains("họp", ignoreCase = true) ||
                        command.contains("đi ", ignoreCase = true) ||
                        command.contains("gửi", ignoreCase = true)
                
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val action = item.optString("action", "ADD").uppercase()
                    
                    val isRepeatCommand = command.contains("mỗi ngày", ignoreCase = true) ||
                            command.contains("hằng ngày", ignoreCase = true) ||
                            command.contains("thói quen", ignoreCase = true) ||
                            command.contains("lặp lại", ignoreCase = true) ||
                            command.contains("mọi ngày", ignoreCase = true) ||
                            item.optString("type").lowercase() == "habit"

                    // Quy tắc: Nếu là lệnh lặp lại thì là thói quen, còn lại MẶC ĐỊNH là Todo
                    val isTodo = !isRepeatCommand || isTodoCommand || item.optString("type").lowercase() == "todo"

                    if (isTodo && !command.contains("thói quen", ignoreCase = true)) {
                        val taskName = item.optString("task").ifBlank {
                            item.optString("name", "Việc cần làm mới")
                        }.trim()
                        
                        val dateStr = item.optString("date").ifBlank {
                            item.optString("dueDate")
                        }
                        val timeStr = item.optString("time")
                        
                        val parsedDate = try {
                            val localDate = if (dateStr.isNotEmpty()) LocalDate.parse(dateStr) else LocalDate.now()
                            val localDateTime = if (timeStr.isNotEmpty()) {
                                try {
                                    val time = java.time.LocalTime.parse(timeStr)
                                    localDate.atTime(time)
                                } catch (e: Exception) {
                                    localDate.atStartOfDay()
                                }
                            } else {
                                localDate.atStartOfDay()
                            }
                            java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
                        } catch (e: Exception) {
                            // Mặc định là hôm nay nếu có lỗi parse hoặc không có ngày
                            java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
                        }

                        if (action == "UPDATE" || action == "MOVE" || command.contains("dời") || command.contains("chuyển")) {
                            // Tìm kiếm việc cần làm hiện tại trùng tên nhất
                            val existingTodo = currentTodos.find { t ->
                                t.task.contains(taskName, ignoreCase = true) || 
                                taskName.contains(t.task, ignoreCase = true)
                            }
                            
                            if (existingTodo != null) {
                                draftTodos.add(existingTodo.copy(dueDate = parsedDate))
                                continue
                            }
                        }

                        // Mặc định là ADD nếu không tìm thấy để UPDATE
                        val priorityStr = item.optString("priority").uppercase()
                        val priority = try {
                            Priority.valueOf(priorityStr)
                        } catch (e: Exception) {
                            Priority.MEDIUM
                        }
                        
                        draftTodos.add(
                            Todo(
                                id = "ai_${System.currentTimeMillis()}_$i",
                                task = taskName,
                                priority = priority,
                                dueDate = parsedDate,
                                createdAt = java.util.Date()
                            )
                        )
                    } else {
                        // Xử lý Habit
                        draftHabits.add(Habit(
                            id = "ai_${System.currentTimeMillis()}_$i",
                            name = item.optString("name", "Thói quen mới"),
                            icon = item.optString("icon", "✨"),
                            goal = "1",
                            colorHex = "#000000", 
                            reminderTime = item.optString("time", "08:00"),
                            reminderMinutesBefore = item.optInt("minsBefore", 5),
                            date = item.optString("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        ))
                    }
                }

                if (draftHabits.isNotEmpty() || draftTodos.isNotEmpty()) {
                    _aiState.value = AiState.Drafting(draftHabits, draftTodos)
                } else {
                    _aiState.value = AiState.Error("Xin lỗi, tôi không tìm thấy thói quen hoặc việc cần làm nào trong câu lệnh của bạn.")
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitsViewModel", "AI Error: ${e.message}")
                _aiState.value = AiState.Error("Lỗi xử lý AI: ${e.localizedMessage}")
            }
        }
    }

    fun confirmAiDrafts(habits: List<Habit>, todos: List<Todo>) {
        viewModelScope.launch {
            habits.forEach { habit ->
                habitRepository.saveHabit(habit)
            }
            todos.forEach { todo ->
                todoRepository.insertTodo(todo)
            }
            _aiState.value = AiState.Success("Cập nhật thành công!")
        }
    }

    fun clearAiState() {
        _aiState.value = AiState.Idle
    }
}
