package com.example.newstart.ui.features.home

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.Priority
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.model.User
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.components.MonthPickerDialog
import com.example.newstart.ui.features.habits.AiState
import com.example.newstart.ui.features.habits.HabitFilter
import com.example.newstart.ui.features.habits.HabitsViewModel
import com.example.newstart.ui.features.habits.components.HabitItem
import com.example.newstart.ui.navigation.Screen
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    habitsViewModel: HabitsViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val habits by habitsViewModel.habits.collectAsStateWithLifecycle()
    val habitsFilter by habitsViewModel.filter.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val selectedDate by mainViewModel.selectedHabitDate.collectAsStateWithLifecycle()
    
    val aiState by habitsViewModel.aiState.collectAsStateWithLifecycle()
    val completedHabitForJournal by habitsViewModel.completedHabitForJournal.collectAsStateWithLifecycle()
    val isJournalPromptEnabled by mainViewModel.isJournalPromptEnabled.collectAsStateWithLifecycle()
    val squads by mainViewModel.squads.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showTodoDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiCommand by remember { mutableStateOf("") }
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Celebration State
    var showCelebration by remember { mutableStateOf(false) }
    val totalHabitsCount = habits.size
    val completedHabitsCount = habits.count { it.isCompleted }
    // Theo dõi trạng thái hoàn thành trước đó để tránh hiện lại khi chuyển tab hoặc mở app
    var wasAllCompleted by remember { 
        mutableStateOf(totalHabitsCount > 0 && completedHabitsCount == totalHabitsCount) 
    }
    var isInitialLoad by remember { mutableStateOf(true) }

    LaunchedEffect(completedHabitsCount, totalHabitsCount) {
        val isCurrentlyAllCompleted = totalHabitsCount > 0 && completedHabitsCount == totalHabitsCount
        
        // Chỉ hiện thông báo nếu trạng thái chuyển từ "chưa xong" sang "đã xong hết" và không phải lần tải đầu tiên
        if (isCurrentlyAllCompleted && !wasAllCompleted && !isInitialLoad) {
            showCelebration = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        
        wasAllCompleted = isCurrentlyAllCompleted
        if (totalHabitsCount > 0) {
            isInitialLoad = false
        }
    }

    // Speech Recognition
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            speechRecognizer.startListening(speechRecognizerIntent)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: android.os.Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    val command = data[0]
                    aiCommand = command
                    habitsViewModel.processAiCommand(command)
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) aiCommand = data[0]
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    val today = LocalDate.now()
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val locale = context.resources.configuration.locales[0]
    val isVietnamese = locale.language == "vi"

    LaunchedEffect(selectedDate) {
        habitsViewModel.onDateSelected(selectedDate)
    }

    // Tự động focus vào Thứ 2 khi chuyển tuần và cập nhật ngày hiển thị
    LaunchedEffect(pagerState.currentPage) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks((pagerState.currentPage - 500).toLong())
        val currentWeekEnd = weekStart.plusDays(6)
        
        // Chỉ cập nhật nếu ngày hiện tại không nằm trong tuần mới đang hiển thị
        if (selectedDate.isBefore(weekStart) || selectedDate.isAfter(currentWeekEnd)) {
            mainViewModel.onHabitDateSelected(weekStart)
        }
    }

    LaunchedEffect(completedHabitForJournal, isJournalPromptEnabled) {
        if (completedHabitForJournal != null && !isJournalPromptEnabled) {
            habitsViewModel.clearJournalPrompt()
        }
    }

    if (showTodoDialog) {
        TodoEditDialog(
            todo = editingTodo,
            onDismiss = {
                showTodoDialog = false
                editingTodo = null
            },
            onConfirm = { task, priority, dueDate ->
                if (editingTodo != null) {
                    viewModel.updateTodo(editingTodo!!.copy(task = task, priority = priority, dueDate = dueDate))
                } else {
                    if (todos.count { !it.isCompleted } >= 50) {
                        android.widget.Toast.makeText(context, "Bạn đã đạt giới hạn 50 nhiệm vụ chưa hoàn thành!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.addTodo(task, priority, dueDate)
                    }
                }
                showTodoDialog = false
                editingTodo = null
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val buttonSizePx = with(density) { 56.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() }

        HomeContent(
            user = userState,
            habits = habits,
            habitsFilter = habitsFilter,
            onFilterSelected = { habitsViewModel.setFilter(it) },
            todos = todos,
            timerSeconds = timerSeconds,
            isTimerRunning = isTimerRunning,
            selectedDate = selectedDate,
            today = today,
            pagerState = pagerState,
            isVietnamese = isVietnamese,
            locale = locale,
            onStopTimer = { viewModel.stopTimer() },
            onToggleHabit = { h, c -> habitsViewModel.toggleHabit(h, c) },
            onDeleteHabit = { habit ->
                scope.launch {
                    habitsViewModel.deleteHabit(habit.id)
                    val result = snackbarHostState.showSnackbar(
                        message = "Đã xóa ${habit.name}",
                        actionLabel = "Hoàn tác",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        habitsViewModel.restoreHabit(habit)
                    }
                }
            },
            onEditHabit = { mainViewModel.startEditingHabit(it) },
            onToggleTodo = { id, c -> viewModel.toggleTodo(id, c) },
            onAddTodo = {
                if (todos.count { !it.isCompleted } >= 50) {
                    android.widget.Toast.makeText(context, "Bạn chỉ được thêm tối đa 50 nhiệm vụ chưa hoàn thành. Hãy hoàn thành bớt việc trước!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    editingTodo = null
                    showTodoDialog = true
                }
            },
            onEditTodo = { todo ->
                editingTodo = todo
                showTodoDialog = true
            },
            onDeleteTodo = { todo ->
                viewModel.deleteTodo(todo)
            },
            onDateSelected = { mainViewModel.onHabitDateSelected(it) },
            onTodayClick = {
                mainViewModel.onHabitDateSelected(today)
                scope.launch { pagerState.animateScrollToPage(500) }
            },
            onShowMonthPicker = { showMonthPicker = true },
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )

        AiFloatingButton(
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            buttonSizePx = buttonSizePx,
            paddingPx = paddingPx,
            onClick = { showAiDialog = true }
        )

        if (showAiDialog || aiState is AiState.Drafting) {
            AiInteractionSheet(
                aiState = aiState,
                aiSheetState = aiSheetState,
                aiCommand = aiCommand,
                onAiCommandChange = { aiCommand = it },
                onDismiss = { 
                    showAiDialog = false
                    aiCommand = ""
                    habitsViewModel.clearAiState()
                },
                onProcessCommand = { habitsViewModel.processAiCommand(it) },
                onConfirmHabits = { habits, todos -> habitsViewModel.confirmAiDrafts(habits, todos) },
                onClearState = { habitsViewModel.clearAiState() },
                isListening = isListening,
                onToggleListening = {
                    if (isListening) {
                        speechRecognizer.stopListening()
                        isListening = false
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isListening = true
                            speechRecognizer.startListening(speechRecognizerIntent)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                squads = squads
            )
        }

        if (showMonthPicker) {
            MonthPickerDialog(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    mainViewModel.onHabitDateSelected(date)
                    showMonthPicker = false
                    val weekDiff = (date.toEpochDay() - today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()) / 7
                    scope.launch { pagerState.scrollToPage(500 + weekDiff.toInt()) }
                },
                onDismiss = { showMonthPicker = false }
            )
        }

        val currentHabit = completedHabitForJournal
        if (isJournalPromptEnabled && currentHabit != null) {
            JournalPromptDialog(
                habitName = currentHabit.name,
                onDismiss = { habitsViewModel.clearJournalPrompt() },
                onConfirm = {
                    habitsViewModel.clearJournalPrompt()
                    mainViewModel.setShowJournalSheet(true)
                    navController.navigate(Screen.Journal.route) {
                        popUpTo(Screen.Home.route) { 
                            saveState = true 
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Celebration Overlay
        AnimatedVisibility(
            visible = showCelebration,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { showCelebration = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🎉", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Thật xuất sắc!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Bạn đã hoàn thành tất cả thói quen của ngày hôm nay.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCelebration = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Tiếp tục duy trì!")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    user: User?,
    habits: List<Habit>,
    habitsFilter: HabitFilter,
    onFilterSelected: (HabitFilter) -> Unit,
    todos: List<Todo>,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    selectedDate: LocalDate,
    today: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    isVietnamese: Boolean,
    locale: Locale,
    onStopTimer: () -> Unit,
    onToggleHabit: (Habit, Boolean) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onToggleTodo: (String, Boolean) -> Unit,
    onAddTodo: () -> Unit,
    onEditTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onShowMonthPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTodos = remember(todos) {
        todos
            .filter { !it.isCompleted }
            .sortedBy {
                when (it.priority) {
                    Priority.HIGH -> 0
                    Priority.MEDIUM -> 1
                    Priority.LOW -> 2
                }
            }
    }
    
    val completedTodos = remember(todos) { todos.filter { it.isCompleted } }
    
    val completedGroups = remember(completedTodos) {
        completedTodos
            .groupBy { todo ->
                val date = todo.createdAt ?: Date()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf.format(date)
            }
            .toList()
            .sortedByDescending { it.first }
    }
    
    var isCompletedExpanded by remember { mutableStateOf(false) }
    var isActiveTodosExpanded by remember { mutableStateOf(false) }

    val handleToggleTodo: (String, Boolean) -> Unit = { id, isCompleted ->
        if (isCompleted) {
            isCompletedExpanded = true
        }
        onToggleTodo(id, isCompleted)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                item { HomeHeaderSection(userName = user?.name ?: "Guest") }
                if (isTimerRunning) {
                    item { TimerCard(timerSeconds, onStopTimer) }
                }
                item { DailyOverviewCard(habits, todos) }
                
                item {
                    HabitsHeader(
                        selectedDate = selectedDate,
                        currentFilter = habitsFilter,
                        onFilterSelected = onFilterSelected,
                        today = today,
                        isVietnamese = isVietnamese,
                        locale = locale,
                        onTodayClick = onTodayClick,
                        onShowMonthPicker = onShowMonthPicker
                    )
                }

                item {
                    HorizontalDatePicker(
                        pagerState = pagerState,
                        selectedDate = selectedDate,
                        today = today,
                        isVietnamese = isVietnamese,
                        locale = locale,
                        onDateClick = onDateSelected
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                if (habits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.habits_empty),
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    val groupedHabits = habits.groupByTimeOfDay()
                    groupedHabits.forEach { (title, habitsInGroup) ->
                        item {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(items = habitsInGroup, key = { it.id }) { habit ->
                            HabitSwipeableItem(
                                modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                                habit = habit,
                                onDelete = { onDeleteHabit(habit) },
                                onToggle = { h, c -> onToggleHabit(h, c) },
                                onEdit = onEditHabit
                            )
                        }
                    }
                }

                item { SectionHeader(title = "Việc cần làm", action = "Thêm", onActionClick = onAddTodo) }
                
                if (activeTodos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tuyệt vời! Bạn không còn việc nào cần làm.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    val visibleActiveTodos = if (isActiveTodosExpanded) activeTodos else activeTodos.take(5)
                    items(items = visibleActiveTodos, key = { it.id }) { todo ->
                        TodoSwipeableItem(
                            todo = todo,
                            onToggle = { handleToggleTodo(todo.id, it) },
                            onClick = { onEditTodo(todo) },
                            onDelete = { onDeleteTodo(todo) },
                            modifier = Modifier.animateItem()
                        )
                    }

                    if (activeTodos.size > 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = { isActiveTodosExpanded = !isActiveTodosExpanded },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isActiveTodosExpanded) "Thu gọn danh sách" else "Xem thêm ${activeTodos.size - 5} việc cần làm khác",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = if (isActiveTodosExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (completedTodos.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCompletedExpanded = !isCompletedExpanded }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Đã hoàn thành (${completedTodos.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isCompletedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    completedGroups.forEach { (dateStr, list) ->
                        if (isCompletedExpanded) {
                            item(key = "header_$dateStr") {
                                val headerText = try {
                                    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val date = sdfInput.parse(dateStr) ?: Date()
                                    val todayStr = sdfInput.format(Date())
                                    val yesterdayStr = sdfInput.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
                                    when (dateStr) {
                                        todayStr -> "Hôm nay"
                                        yesterdayStr -> "Hôm qua"
                                        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                                    }
                                } catch (e: Exception) {
                                    dateStr
                                }
                                Text(
                                    text = headerText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp, vertical = 6.dp)
                                        .animateItem()
                                )
                            }

                            items(items = list, key = { it.id }) { todo ->
                                TodoSwipeableItem(
                                    todo = todo,
                                    onToggle = { handleToggleTodo(todo.id, it) },
                                    onClick = { onEditTodo(todo) },
                                    onDelete = { onDeleteTodo(todo) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<Habit>.groupByTimeOfDay(): List<Pair<String, List<Habit>>> {
    val morning = mutableListOf<Habit>()
    val afternoon = mutableListOf<Habit>()
    val evening = mutableListOf<Habit>()
    val other = mutableListOf<Habit>()

    this.forEach { habit ->
        val time = habit.reminderTime
        if (time == null) {
            other.add(habit)
        } else {
            val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: -1
            when (hour) {
                in 0..11 -> morning.add(habit)
                in 12..17 -> afternoon.add(habit)
                in 18..23 -> evening.add(habit)
                else -> other.add(habit)
            }
        }
    }
    return listOf(
        "Buổi sáng" to morning,
        "Buổi chiều" to afternoon,
        "Buổi tối" to evening,
        "Khác" to other
    ).filter { it.second.isNotEmpty() }
}

@Composable
private fun HabitSwipeableItem(
    modifier: Modifier = Modifier,
    habit: Habit,
    onDelete: () -> Unit,
    onToggle: (Habit, Boolean) -> Unit,
    onEdit: (Habit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val anchorWidth = with(density) { 80.dp.toPx() }
    val dismissThreshold = with(density) { 180.dp.toPx() }
    
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF4444))
                .clickable { 
                    scope.launch {
                        offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                        onDelete()
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier.width(80.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(habit.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = (offsetX.value + dragAmount).coerceAtMost(0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                            change.consume()
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -dismissThreshold) {
                                    offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                                    onDelete()
                                } else if (offsetX.value < -anchorWidth / 2) {
                                    offsetX.animateTo(-anchorWidth, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                }
                            }
                        }
                    )
                }
        ) {
            HabitItem(
                habit = habit, 
                onToggle = { onToggle(habit, !habit.isCompleted) }, 
                onEdit = { onEdit(habit) }
            )
        }
    }
}

@Composable
private fun HabitsHeader(
    selectedDate: LocalDate,
    currentFilter: HabitFilter,
    onFilterSelected: (HabitFilter) -> Unit,
    today: LocalDate,
    isVietnamese: Boolean,
    locale: Locale,
    onTodayClick: () -> Unit,
    onShowMonthPicker: () -> Unit,
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showFilterMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (currentFilter) {
                            HabitFilter.ALL -> stringResource(R.string.habits_filter_all)
                            HabitFilter.COMPLETED -> stringResource(R.string.habits_filter_completed)
                            HabitFilter.UNCOMPLETED -> stringResource(R.string.habits_filter_uncompleted)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.habits_filter_all)) },
                        onClick = {
                            onFilterSelected(HabitFilter.ALL)
                            showFilterMenu = false
                        },
                        leadingIcon = {
                            if (currentFilter == HabitFilter.ALL) Icon(Icons.Default.Check, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.habits_filter_completed)) },
                        onClick = {
                            onFilterSelected(HabitFilter.COMPLETED)
                            showFilterMenu = false
                        },
                        leadingIcon = {
                            if (currentFilter == HabitFilter.COMPLETED) Icon(Icons.Default.Check, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.habits_filter_uncompleted)) },
                        onClick = {
                            onFilterSelected(HabitFilter.UNCOMPLETED)
                            showFilterMenu = false
                        },
                        leadingIcon = {
                            if (currentFilter == HabitFilter.UNCOMPLETED) Icon(Icons.Default.Check, null)
                        }
                    )
                }
            }
        }

        val headerDateText = remember(selectedDate, locale) {
            if (selectedDate == today) null
            else selectedDate.format(DateTimeFormatter.ofPattern(if (isVietnamese) "dd MMMM" else "MMM dd", locale))
        }

        Text(
            text = headerDateText ?: stringResource(R.string.habits_today),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTodayClick() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = onShowMonthPicker,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalDatePicker(
    pagerState: androidx.compose.foundation.pager.PagerState,
    selectedDate: LocalDate,
    today: LocalDate,
    isVietnamese: Boolean,
    locale: Locale,
    onDateClick: (LocalDate) -> Unit
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { page ->
        val weekStart = remember(page) {
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks((page - 500).toLong())
        }
        val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            weekDays.forEach { day ->
                val isSelected = day == selectedDate
                val isToday = day == today
                val dayName = remember(day, locale) {
                    if (isVietnamese) {
                        when (day.dayOfWeek) {
                            DayOfWeek.MONDAY -> "T2"
                            DayOfWeek.TUESDAY -> "T3"
                            DayOfWeek.WEDNESDAY -> "T4"
                            DayOfWeek.THURSDAY -> "T5"
                            DayOfWeek.FRIDAY -> "T6"
                            DayOfWeek.SATURDAY -> "T7"
                            DayOfWeek.SUNDAY -> "CN"
                        }
                    } else day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale).uppercase()
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { onDateClick(day) }) {
                    Text(text = dayName, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = if (!isSelected && isToday) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = day.dayOfMonth.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoSwipeableItem(
    todo: Todo,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val anchorWidth = with(density) { 70.dp.toPx() }
    val dismissThreshold = with(density) { 180.dp.toPx() }
    
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Red)
                .clickable { 
                    scope.launch {
                        offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                        onDelete()
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier.width(70.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(todo.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = (offsetX.value + dragAmount).coerceAtMost(0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                            change.consume()
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -dismissThreshold) {
                                    offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                                    onDelete()
                                } else if (offsetX.value < -anchorWidth / 2) {
                                    offsetX.animateTo(-anchorWidth, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                }
                            }
                        }
                    )
                }
        ) {
            HomeTodoItem(
                todo = todo,
                onToggle = onToggle,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun HomeHeaderSection(userName: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..10 -> R.string.home_hello_morning
            in 11..13 -> R.string.home_hello_noon
            in 14..17 -> R.string.home_hello_afternoon
            else -> R.string.home_hello_evening
        }
        Text(text = stringResource(id = greetingRes, userName), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun DailyOverviewCard(habits: List<Habit>, todos: List<Todo>) {
    val total = habits.size + todos.size
    val completed = habits.count { it.isCompleted } + todos.count { it.isCompleted }
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val percent = (progress * 100).toInt()

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp, trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                Text("$percent%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Tiến độ ngày", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Bạn đã hoàn thành $completed/$total mục tiêu.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun TimerCard(seconds: Int, onStop: () -> Unit) {
    val mins = seconds / 60
    val secs = seconds % 60
    val timeStr = String.format("%02d:%02d", mins, secs)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Đang tập trung", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                Text(timeStr, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Dừng", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HomeTodoItem(
    todo: Todo, 
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val priorityColor = when (todo.priority) {
        Priority.HIGH -> Color(0xFFFF4444)
        Priority.MEDIUM -> Color(0xFFFFBB33)
        Priority.LOW -> Color(0xFF00C851)
    }

    val isOverdue = remember(todo.dueDate, todo.isCompleted) {
        todo.dueDate?.let {
            val calToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calDue = Calendar.getInstance().apply {
                time = it
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            !todo.isCompleted && calDue.before(calToday)
        } ?: false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp, 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), 
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(priorityColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                text = todo.task, 
                fontSize = 14.sp,
                textDecoration = if (todo.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null, 
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            if (todo.dueDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isOverdue) Color(0xFFFF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOverdue) "Quá hạn: ${sdf.format(todo.dueDate)}" else sdf.format(todo.dueDate),
                        fontSize = 11.sp,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) Color(0xFFFF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Checkbox(
            checked = todo.isCompleted, 
            onCheckedChange = onToggle, 
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onActionClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            text = action, 
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditDialog(
    todo: Todo?,
    onDismiss: () -> Unit,
    onConfirm: (String, Priority, Date?) -> Unit
) {
    var task by remember { mutableStateOf(todo?.task ?: "") }
    var priority by remember { mutableStateOf(todo?.priority ?: Priority.MEDIUM) }
    var dueDate by remember { mutableStateOf<Date?>(todo?.dueDate) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        content = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = if (todo == null) "Việc cần làm mới" else "Sửa công việc",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = task,
                            onValueChange = { if (it.length <= 50) task = it },
                            placeholder = { Text("Bạn định làm gì thế?", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${task.length}/50",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.length >= 45) Color(0xFFFF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (task.length >= 45) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Mức độ quan trọng",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Priority.entries.forEach { p ->
                                val isSelected = priority == p
                                val pColor = when (p) {
                                    Priority.HIGH -> Color(0xFFFF4444)
                                    Priority.MEDIUM -> Color(0xFFFFBB33)
                                    Priority.LOW -> Color(0xFF00C851)
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { priority = p },
                                    label = { 
                                        Text(
                                            when(p) {
                                                Priority.HIGH -> "Cao"
                                                Priority.MEDIUM -> "Vừa"
                                                Priority.LOW -> "Thấp"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        ) 
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = pColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = pColor.copy(alpha = 0.1f),
                                        labelColor = pColor
                                    ),
                                    border = null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Hạn hoàn thành",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                            val dateText = dueDate?.let { sdf.format(it) } ?: "Không có hạn chót"
                            
                            val showDatePicker = {
                                val calendar = Calendar.getInstance()
                                dueDate?.let { calendar.time = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance()
                                        newCal.set(year, month, dayOfMonth)
                                        dueDate = newCal.time
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }

                            Surface(
                                onClick = showDatePicker,
                                shape = RoundedCornerShape(12.dp),
                                color = if (dueDate != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (dueDate != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = dateText,
                                        fontSize = 13.sp,
                                        fontWeight = if (dueDate != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (dueDate != null) {
                                IconButton(
                                    onClick = { dueDate = null },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Red.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Gỡ hạn chót",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Button(
                            onClick = { if (task.isNotBlank()) onConfirm(task, priority, dueDate) },
                            enabled = task.isNotBlank(),
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("Lưu", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun AiFloatingButton(
    maxWidth: Float,
    maxHeight: Float,
    buttonSizePx: Float,
    paddingPx: Float,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val navBarHeightPx = with(density) { 80.dp.toPx() }
    
    val offsetX = remember(maxWidth) { Animatable(maxWidth - buttonSizePx - paddingPx) }
    val offsetY = remember(maxHeight) { Animatable(maxHeight - buttonSizePx - paddingPx - navBarHeightPx) }

    Surface(
        modifier = Modifier
            .size(56.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .pointerInput(maxWidth, maxHeight) {
                detectDragGestures(
                    onDragEnd = {
                        val centerX = offsetX.value + buttonSizePx / 2
                        val targetX = if (centerX < maxWidth / 2) paddingPx else maxWidth - buttonSizePx - paddingPx
                        scope.launch {
                            offsetX.animateTo(targetX, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newX = (offsetX.value + dragAmount.x).coerceIn(paddingPx, maxWidth - buttonSizePx - paddingPx)
                            val newY = (offsetY.value + dragAmount.y).coerceIn(paddingPx, maxHeight - buttonSizePx - paddingPx - navBarHeightPx)
                            offsetX.snapTo(newX)
                            offsetY.snapTo(newY)
                        }
                    }
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(0.5.dp, Brush.linearGradient(listOf(Color(0xFF007AFF).copy(alpha = 0.5f), Color(0xFF00C851).copy(alpha = 0.5f)))),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.AutoAwesome, 
                contentDescription = "AI Assistant", 
                modifier = Modifier.size(24.dp), 
                tint = Color(0xFF007AFF)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiInteractionSheet(
    aiState: AiState,
    aiSheetState: SheetState,
    aiCommand: String,
    onAiCommandChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onProcessCommand: (String) -> Unit,
    onConfirmHabits: (List<Habit>, List<Todo>) -> Unit,
    onClearState: () -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit,
    squads: List<com.example.newstart.domain.model.Squad> = emptyList()
) {
    val aiGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF007AFF), Color(0xFF00C851))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = aiSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AnimatedContent(targetState = aiState, label = "ai_content_anim") { state ->
            when (state) {
                is AiState.Drafting -> AiDraftingView(state.habits, state.todos, squads, onConfirmHabits, onClearState)
                else -> AiInputView(state, aiCommand, onAiCommandChange, onProcessCommand, isListening, onToggleListening, aiGradient)
            }
        }
    }
}

@Composable
private fun AiDraftingView(
    initialHabits: List<Habit>,
    initialTodos: List<Todo>,
    squads: List<com.example.newstart.domain.model.Squad> = emptyList(),
    onConfirm: (List<Habit>, List<Todo>) -> Unit,
    onCancel: () -> Unit
) {
    var draftHabits by remember(initialHabits) { mutableStateOf(initialHabits) }
    var draftTodos by remember(initialTodos) { mutableStateOf(initialTodos) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    if (editingHabit != null && editingIndex != -1) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    editingHabit = null
                    editingIndex = -1
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Chỉnh sửa thói quen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            com.example.newstart.ui.features.habits.components.HabitConfigContent(
                initialDate = LocalDate.parse(editingHabit!!.date),
                habit = editingHabit!!,
                squads = squads,
                onConfirm = { name, icon, time, mins, color, date, squadId ->
                    val colorHex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                    val updatedHabit = editingHabit!!.copy(
                        name = name,
                        icon = icon,
                        reminderTime = time,
                        reminderMinutesBefore = mins,
                        colorHex = colorHex,
                        date = date.toString(),
                        squadId = squadId
                    )
                    draftHabits = draftHabits.toMutableList().apply {
                        set(editingIndex, updatedHabit)
                    }
                    editingHabit = null
                    editingIndex = -1
                },
                onCancel = {
                    editingHabit = null
                    editingIndex = -1
                }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Đề xuất từ AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("AI đề xuất các mục sau", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        
        if (draftHabits.isNotEmpty()) {
            Text(
                text = "Thói quen đề xuất (Bấm để sửa)", 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            draftHabits.forEachIndexed { index, habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable {
                            editingHabit = habit
                            editingIndex = index
                        },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(habit.icon, fontSize = 24.sp)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            habit.reminderTime?.let { time ->
                                Text(time, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        if (draftTodos.isNotEmpty()) {
            Text(
                text = "Việc cần làm đề xuất", 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.align(Alignment.Start).padding(top = 12.dp, bottom = 8.dp)
            )
            draftTodos.forEachIndexed { index, todo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(todo.task, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Độ ưu tiên: ${todo.priority.name}", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(draftHabits, draftTodos) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Thêm tất cả vào ứng dụng", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Hủy bỏ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AiInputView(
    state: AiState,
    command: String,
    onCommandChange: (String) -> Unit,
    onSend: (String) -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit,
    gradient: Brush
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("NewStart AI", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, brush = gradient))
        }
        Text("Tôi có thể giúp bạn lên lịch thói quen chỉ bằng một câu nói.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = command,
            onValueChange = onCommandChange,
            placeholder = { Text("Bạn đang muốn làm gì?") },
            modifier = Modifier.fillMaxWidth().border(width = 1.dp, brush = if (command.isNotBlank() || isListening) gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                IconButton(onClick = { onToggleListening() }) {
                    Icon(imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone, contentDescription = "Voice", tint = if (isListening) Color.Red else Color(0xFF007AFF))
                }
            },
            trailingIcon = {
                IconButton(onClick = { if (command.isNotBlank()) onSend(command) }, enabled = command.isNotBlank() && state !is AiState.Loading) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (command.isNotBlank()) Color(0xFF007AFF) else Color.Gray)
                }
            }
        )
        if (state is AiState.Loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(2.dp)
                    .clip(CircleShape), 
                color = Color(0xFF007AFF)
            )
        }
        
        if (state is AiState.Error) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (state is AiState.Success) {
            Text(
                text = state.message,
                color = Color(0xFF00C851),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun JournalPromptDialog(habitName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tuyệt vời!") },
        text = { Text("Bạn vừa hoàn thành '$habitName'. Bạn có muốn lưu lại khoảnh khắc này vào nhật ký không?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Viết nhật ký ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau")
            }
        }
    )
}
