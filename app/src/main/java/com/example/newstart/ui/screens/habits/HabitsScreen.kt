package com.example.newstart.ui.screens.habits

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.screens.journal.MonthPickerDialog
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val selectedDate by mainViewModel.selectedHabitDate.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    
    val scope = rememberCoroutineScope()
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiCommand by remember { mutableStateOf("") }
    
    val today = LocalDate.now()
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)

    val locale = LocalContext.current.resources.configuration.locales[0]
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                    onClick = { /* All filter */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.habits_filter_all),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                    }
                }

                val headerDateText = remember(selectedDate, locale) {
                    if (selectedDate == today) null
                    else {
                        val pattern = if (isVietnamese) "dd MMMM" else "MMM dd"
                        selectedDate.format(DateTimeFormatter.ofPattern(pattern, locale))
                    }
                }

                Text(
                    text = headerDateText ?: stringResource(R.string.habits_today),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            mainViewModel.onHabitDateSelected(today)
                            scope.launch {
                                pagerState.animateScrollToPage(500)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp),
                    onClick = { showMonthPicker = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Month Picker",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Horizontal Date Picker
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) { page ->
                val weekStart = remember(page) {
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusWeeks((page - 500).toLong())
                }
                val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                            } else {
                                day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale).uppercase()
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mainViewModel.onHabitDateSelected(day) }
                        ) {
                            Text(
                                text = dayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (!isSelected && isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Habit List
            if (habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.habits_empty),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = habits,
                        key = { it.id }
                    ) { habit ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    habitToDelete = habit
                                    false
                                } else false
                            }
                        )

                        LaunchedEffect(habitToDelete) {
                            if (habitToDelete == null) {
                                dismissState.reset()
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                                val backgroundColor = if (isSwiping && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                    Color.Red.copy(alpha = 0.8f)
                                } else {
                                    Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(backgroundColor),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (isSwiping && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                }
                            },
                            content = {
                                HabitItem(
                                    habit = habit,
                                    onToggle = { viewModel.toggleHabit(habit, !habit.isCompleted) },
                                    onEdit = {
                                        mainViewModel.startEditingHabit(habit)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Floating Modern AI Button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
                .size(56.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4285F4), Color(0xFF9B72CB), Color(0xFFD96570))
                    ),
                    shape = CircleShape
                ),
            onClick = { showAiDialog = true }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF9B72CB)
                )
            }
        }

        // AI Interaction Flow (Modal Bottom Sheet)
        if (showAiDialog || aiState is AiState.Drafting) {
            val aiGradient = Brush.linearGradient(
                colors = listOf(Color(0xFF4285F4), Color(0xFF9B72CB), Color(0xFFD96570))
            )

            ModalBottomSheet(
                onDismissRequest = { 
                    showAiDialog = false
                    aiCommand = ""
                    viewModel.clearAiState()
                },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                AnimatedContent(
                    targetState = aiState,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "ai_content_anim"
                ) { targetState ->
                    when (targetState) {
                        is AiState.Drafting -> {
                            val drafts = targetState.habits
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Xác nhận thói quen",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "AI đã đề xuất ${drafts.size} mục mới cho bạn",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                LazyColumn(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(drafts) { habit ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                                
                                                Spacer(modifier = Modifier.width(16.dp))
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    if (habit.reminderTime != null) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(habit.reminderTime!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                                
                                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Button(
                                    onClick = { 
                                        viewModel.confirmAiHabits(drafts)
                                        showAiDialog = false
                                        aiCommand = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Thêm tất cả vào lịch trình", fontWeight = FontWeight.Bold)
                                }
                                
                                TextButton(
                                    onClick = { viewModel.clearAiState() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text("Hủy bỏ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome, 
                                        null, 
                                        tint = Color(0xFF9B72CB),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "NewStart AI",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            brush = aiGradient
                                        )
                                    )
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Tôi có thể giúp bạn lên lịch thói quen chỉ bằng một câu nói.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Ăn sáng 7h", "Gym 17h", "Đọc sách 22h").forEach { suggestion ->
                                        AssistChip(
                                            onClick = { aiCommand = "Thêm thói quen $suggestion" },
                                            label = { Text(suggestion, fontSize = 12.sp) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = aiCommand,
                                    onValueChange = { aiCommand = it },
                                    placeholder = { Text("Bạn đang muốn làm gì?") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            brush = if (aiCommand.isNotBlank()) aiGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { 
                                                if (aiCommand.isNotBlank()) {
                                                    viewModel.processAiCommand(aiCommand) 
                                                }
                                            },
                                            enabled = aiCommand.isNotBlank() && targetState !is AiState.Loading
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send, 
                                                contentDescription = "Send",
                                                tint = if (aiCommand.isNotBlank()) Color(0xFF9B72CB) else Color.Gray
                                            )
                                        }
                                    },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onSend = { viewModel.processAiCommand(aiCommand) }
                                    )
                                )

                                if (targetState is AiState.Loading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp)
                                            .height(2.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF9B72CB),
                                        trackColor = Color(0xFF9B72CB).copy(alpha = 0.1f)
                                    )
                                }

                                if (targetState is AiState.Error) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        targetState.message, 
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (targetState is AiState.Success) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        targetState.message, 
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        habitToDelete?.let { habit ->
            AlertDialog(
                onDismissRequest = { habitToDelete = null },
                title = { Text(stringResource(R.string.habits_delete_title)) },
                text = { Text(stringResource(R.string.habits_delete_msg, habit.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit(habit.id)
                            habitToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.habits_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { habitToDelete = null }) {
                        Text(stringResource(R.string.habits_cancel))
                    }
                }
            )
        }

        // Monthly Calendar Dialog
        if (showMonthPicker) {
            MonthPickerDialog(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    mainViewModel.onHabitDateSelected(date)
                    showMonthPicker = false
                    val weekDiff = (date.toEpochDay() - today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()) / 7
                    scope.launch {
                        pagerState.scrollToPage(500 + weekDiff.toInt())
                    }
                },
                onDismiss = { showMonthPicker = false }
            )
        }
    }
}

@Composable
fun HabitItem(
    habit: Habit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val color = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF1D5FE2)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = if (habit.isCompleted) 1f else 0.12f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(habit.icon, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.name,
                        color = if (habit.isCompleted) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (habit.streak > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.LocalFireDepartment, 
                            null, 
                            tint = Color(0xFFFFA500),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "${habit.streak} " + stringResource(R.string.habits_streak_day),
                            color = if (habit.isCompleted) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = if (habit.reminderTime != null) {
                        "${habit.progress}/${habit.goal} • 🔔 ${habit.reminderTime}"
                    } else {
                        "${habit.progress}/${habit.goal}"
                    },
                    color = if (habit.isCompleted) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.2.dp,
                        color = if (habit.isCompleted) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .background(if (habit.isCompleted) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (habit.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
