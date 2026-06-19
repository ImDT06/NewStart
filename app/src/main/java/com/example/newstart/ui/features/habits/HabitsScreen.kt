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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.navigation.Screen
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
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val selectedDate by mainViewModel.selectedHabitDate.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val completedHabitForJournal by viewModel.completedHabitForJournal.collectAsStateWithLifecycle()
    val isJournalPromptEnabled by mainViewModel.isJournalPromptEnabled.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    var showMonthPicker by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiCommand by remember { mutableStateOf("") }
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Celebration State
    var showCelebration by remember { mutableStateOf(false) }
    val totalCount = habits.size
    val completedCount = habits.count { it.isCompleted }

    LaunchedEffect(completedCount, totalCount) {
        if (totalCount > 0 && (completedCount == totalCount)) {
            showCelebration = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            showCelebration = false
        }
    }

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

    val today = LocalDate.now()
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val locale = context.resources.configuration.locales[0]
    val isVietnamese = locale.language == "vi"

    LaunchedEffect(selectedDate) {
        viewModel.onDateSelected(selectedDate)
    }

    LaunchedEffect(completedHabitForJournal, isJournalPromptEnabled) {
        if (completedHabitForJournal != null && !isJournalPromptEnabled) {
            viewModel.clearJournalPrompt()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val buttonSizePx = with(density) { 56.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() }

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
                navController = navController,
                onTodayClick = {
                    mainViewModel.onHabitDateSelected(today)
                    scope.launch { pagerState.animateScrollToPage(500) }
                },
                onShowMonthPicker = { showMonthPicker = true }
            )
            
            HabitProgressDashboard(habits = habits)

            HorizontalDatePicker(
                pagerState = pagerState,
                selectedDate = selectedDate,
                today = today,
                isVietnamese = isVietnamese,
                locale = locale,
                onDateClick = { mainViewModel.onHabitDateSelected(it) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            HabitList(
                habits = habits,
                onDelete = { habit ->
                    scope.launch {
                        viewModel.deleteHabit(habit.id)
                        val result = snackbarHostState.showSnackbar(
                            message = "Đã xóa ${habit.name}",
                            actionLabel = "Hoàn tác",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreHabit(habit)
                        }
                    }
                },
                onToggle = { h, c -> viewModel.toggleHabit(h, c) },
                onEdit = { mainViewModel.startEditingHabit(it) }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )

        AiFloatingButton(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
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
                onDismiss = { viewModel.clearJournalPrompt() },
                onConfirm = {
                    viewModel.clearJournalPrompt()
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

@Composable
private fun HabitProgressDashboard(habits: List<Habit>) {
    val completedCount = habits.count { it.isCompleted }
    val totalCount = habits.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 6.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = when {
                            totalCount == 0 -> "Bắt đầu ngày mới!"
                            progress == 1f -> "Hoàn hảo! 🏆"
                            progress > 0.5f -> "Sắp xong rồi!"
                            else -> "Tiến độ hôm nay"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (totalCount == 0) "Thêm thói quen để bắt đầu"
                        else if (progress == 1f) "Bạn đã hoàn thành tất cả!"
                        else "Cố lên, còn ${totalCount - completedCount} mục tiêu nữa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (totalCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val longestStreak = habits.maxOfOrNull { it.streak } ?: 0
                QuickStatItem(
                    label = "Chuỗi cao nhất",
                    value = "$longestStreak ngày",
                    icon = Icons.Default.Whatshot,
                    color = Color(0xFFFFA500),
                    modifier = Modifier.weight(1f)
                )
                QuickStatItem(
                    label = "Đã hoàn thành",
                    value = "$completedCount/$totalCount",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF00C851),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitsHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    isVietnamese: Boolean,
    locale: java.util.Locale,
    navController: NavController,
    onTodayClick: () -> Unit,
    onShowMonthPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp)) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.habits_filter_all), color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(10.dp))
            }
        }

        val headerDateText = remember(selectedDate, locale) {
            if (selectedDate == today) null
            else selectedDate.format(DateTimeFormatter.ofPattern(if (isVietnamese) "dd MMMM" else "MMM dd", locale))
        }

        Text(
            text = headerDateText ?: stringResource(R.string.habits_today),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onTodayClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        IconButton(onClick = onShowMonthPicker) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        IconButton(onClick = { navController.navigate(Screen.Statistics.route) }) {
            Icon(Icons.Default.AutoGraph, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
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
private fun HabitList(
    habits: List<Habit>,
    onDelete: (Habit) -> Unit,
    onToggle: (Habit, Boolean) -> Unit,
    onEdit: (Habit) -> Unit
) {
    if (habits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.habits_empty),
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        val groupedHabits = remember(habits) {
            val morning = mutableListOf<Habit>()
            val afternoon = mutableListOf<Habit>()
            val evening = mutableListOf<Habit>()
            val other = mutableListOf<Habit>()

            habits.forEach { habit ->
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
            listOf(
                "Buổi sáng" to morning,
                "Buổi chiều" to afternoon,
                "Buổi tối" to evening,
                "Khác" to other
            ).filter { it.second.isNotEmpty() }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedHabits.forEach { (title, habitsInGroup) ->
                item {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }
                items(items = habitsInGroup, key = { it.id }) { habit ->
                    HabitSwipeableItem(
                        modifier = Modifier.animateItem(),
                        habit = habit,
                        onDelete = { onDelete(habit) },
                        onToggle = onToggle,
                        onEdit = onEdit
                    )
                }
            }
        }
    }
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
                .clip(RoundedCornerShape(16.dp))
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
private fun AiFloatingButton(
    maxWidth: Float,
    maxHeight: Float,
    buttonSizePx: Float,
    paddingPx: Float,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val navBarHeightPx = with(density) { 56.dp.toPx() }
    
    val offsetX = remember { Animatable(maxWidth - buttonSizePx - paddingPx) }
    val offsetY = remember { Animatable(maxHeight - buttonSizePx - paddingPx - navBarHeightPx) }

    Surface(
        modifier = Modifier
            .size(52.dp)
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
    onConfirmHabits: (List<Habit>) -> Unit,
    onClearState: () -> Unit,
    isListening: Boolean,
    onToggleListening: () -> Unit
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
