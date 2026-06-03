package com.example.newstart.ui.features.habits

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.components.MonthPickerDialog
import com.example.newstart.ui.features.habits.components.HabitItem
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val selectedDate by mainViewModel.selectedHabitDate.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiCommand by remember { mutableStateOf("") }
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
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
                    viewModel.processAiCommand(command)
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

    var aiButtonOffset by remember { mutableStateOf(Offset.Zero) }
    val today = LocalDate.now()
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val locale = context.resources.configuration.locales[0]
    val isVietnamese = locale.language == "vi"

    LaunchedEffect(selectedDate) {
        viewModel.onDateSelected(selectedDate)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            HabitsHeader(
                selectedDate = selectedDate,
                today = today,
                isVietnamese = isVietnamese,
                locale = locale,
                onTodayClick = {
                    mainViewModel.onHabitDateSelected(today)
                    scope.launch { pagerState.animateScrollToPage(500) }
                },
                onShowMonthPicker = { showMonthPicker = true }
            )

            HorizontalDatePicker(
                pagerState = pagerState,
                selectedDate = selectedDate,
                today = today,
                isVietnamese = isVietnamese,
                locale = locale,
                onDateClick = { mainViewModel.onHabitDateSelected(it) }
            )

            HabitList(
                habits = habits,
                onDeleteRequest = { habitToDelete = it },
                onToggle = { h, c -> viewModel.toggleHabit(h, c) },
                onEdit = { mainViewModel.startEditingHabit(it) }
            )
        }

        AiFloatingButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            offset = aiButtonOffset,
            onOffsetChange = { aiButtonOffset = it },
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
                    viewModel.clearAiState()
                },
                onProcessCommand = { viewModel.processAiCommand(it) },
                onConfirmHabits = { viewModel.confirmAiHabits(it) },
                onClearState = { viewModel.clearAiState() },
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
                }
            )
        }

        if (habitToDelete != null) {
            DeleteHabitDialog(
                habit = habitToDelete!!,
                onDismiss = { habitToDelete = null },
                onConfirm = {
                    viewModel.deleteHabit(habitToDelete!!.id)
                    habitToDelete = null
                }
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
    }
}

@Composable
private fun HabitsHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    isVietnamese: Boolean,
    locale: java.util.Locale,
    onTodayClick: () -> Unit,
    onShowMonthPicker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.habits_filter_all), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
            }
        }

        val headerDateText = remember(selectedDate, locale) {
            if (selectedDate == today) null
            else selectedDate.format(DateTimeFormatter.ofPattern(if (isVietnamese) "dd MMMM" else "MMM dd", locale))
        }

        Text(
            text = headerDateText ?: stringResource(R.string.habits_today),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onTodayClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        IconButton(onClick = onShowMonthPicker) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
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
    locale: java.util.Locale,
    onDateClick: (LocalDate) -> Unit
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { page ->
        val weekStart = remember(page) {
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks((page - 500).toLong())
        }
        val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
                    Text(text = dayName, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = if (!isSelected && isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = day.dayOfMonth.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitList(
    habits: List<Habit>,
    onDeleteRequest: (Habit) -> Unit,
    onToggle: (Habit, Boolean) -> Unit,
    onEdit: (Habit) -> Unit
) {
    if (habits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.habits_empty), color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = habits, key = { it.id }) { habit ->
                HabitSwipeableItem(habit = habit, onDeleteRequest = onDeleteRequest, onToggle = onToggle, onEdit = onEdit)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitSwipeableItem(
    habit: Habit,
    onDeleteRequest: (Habit) -> Unit,
    onToggle: (Habit, Boolean) -> Unit,
    onEdit: (Habit) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest(habit)
                false
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val backgroundColor = if (isSwiping && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red.copy(alpha = 0.8f) else Color.Transparent
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(backgroundColor), contentAlignment = Alignment.CenterEnd) {
                if (isSwiping && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                }
            }
        },
        content = {
            HabitItem(habit = habit, onToggle = { onToggle(habit, !habit.isCompleted) }, onEdit = { onEdit(habit) })
        }
    )
}

@Composable
private fun AiFloatingButton(
    modifier: Modifier = Modifier,
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onOffsetChange(offset + dragAmount) } }
            .padding(end = 20.dp, bottom = 100.dp)
            .size(56.dp)
            .border(2.dp, Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF9B72CB))), CircleShape),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, "AI Assistant", modifier = Modifier.size(28.dp), tint = Color(0xFF9B72CB))
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
    onConfirmHabits: (List<Habit>) -> Unit,
    onClearState: () -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit
) {
    val aiGradient = Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF9B72CB)))
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = aiSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AnimatedContent(targetState = aiState, label = "ai_content_anim") { state ->
            when (state) {
                is AiState.Drafting -> AiDraftingView(state.habits, onConfirmHabits, onClearState)
                else -> AiInputView(state, aiCommand, onAiCommandChange, onProcessCommand, isListening, onToggleListening, aiGradient)
            }
        }
    }
}

@Composable
private fun AiDraftingView(habits: List<Habit>, onConfirm: (List<Habit>) -> Unit, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Xác nhận thói quen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("AI đã đề xuất ${habits.size} mục mới cho bạn", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        habits.forEach { habit ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                        Box(contentAlignment = Alignment.Center) { Text(habit.icon, fontSize = 24.sp) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        habit.reminderTime?.let { time ->
                            Text(time, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Button(onClick = { onConfirm(habits) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Thêm tất cả vào lịch trình", fontWeight = FontWeight.Bold)
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
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF9B72CB), modifier = Modifier.size(24.dp))
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
                IconButton(onClick = onToggleListening) {
                    Icon(imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone, contentDescription = "Voice", tint = if (isListening) Color.Red else Color(0xFF9B72CB))
                }
            },
            trailingIcon = {
                IconButton(onClick = { if (command.isNotBlank()) onSend(command) }, enabled = command.isNotBlank() && state !is AiState.Loading) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (command.isNotBlank()) Color(0xFF9B72CB) else Color.Gray)
                }
            }
        )
        if (state is AiState.Loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(2.dp).clip(CircleShape), color = Color(0xFF9B72CB))
    }
}

@Composable
private fun DeleteHabitDialog(habit: Habit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habits_delete_title)) },
        text = { Text(stringResource(R.string.habits_delete_msg, habit.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.habits_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.habits_cancel)) }
        }
    )
}
