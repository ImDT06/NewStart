package com.example.newstart.ui.features.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.Priority
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.model.User
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val habits by viewModel.todayHabits.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()

    var showTodoDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }

    if (showTodoDialog) {
        TodoEditDialog(
            todo = editingTodo,
            onDismiss = {
                showTodoDialog = false
                editingTodo = null
            },
            onConfirm = { task, priority ->
                if (editingTodo != null) {
                    viewModel.updateTodo(editingTodo!!.copy(task = task, priority = priority))
                } else {
                    viewModel.addTodo(task, priority)
                }
                showTodoDialog = false
                editingTodo = null
            }
        )
    }

    HomeContent(
        user = userState,
        habits = habits,
        todos = todos,
        timerSeconds = timerSeconds,
        isTimerRunning = isTimerRunning,
        onStopTimer = { viewModel.stopTimer() },
        onToggleHabit = { h, c -> viewModel.toggleHabit(h, c) },
        onToggleTodo = { id, c -> viewModel.toggleTodo(id, c) },
        onAddTodo = {
            editingTodo = null
            showTodoDialog = true
        },
        onEditTodo = { todo ->
            editingTodo = todo
            showTodoDialog = true
        },
        onDeleteTodo = { todo ->
            viewModel.deleteTodo(todo)
        },
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    user: User?,
    habits: List<Habit>,
    todos: List<Todo>,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    onStopTimer: () -> Unit,
    onToggleHabit: (Habit, Boolean) -> Unit,
    onToggleTodo: (String, Boolean) -> Unit,
    onAddTodo: () -> Unit,
    onEditTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                item { HomeHeaderSection(userName = user?.name ?: "Guest") }
                if (isTimerRunning) {
                    item { TimerCard(timerSeconds, onStopTimer) }
                }
                item { DailyOverviewCard(habits, todos) }
                item { SectionHeader(title = "Thói quen hôm nay", action = "Tất cả", onActionClick = {})
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(items = habits, key = { it.id }) { habit -> 
                            InstagramHabitItem(habit, onToggle = { onToggleHabit(habit, it) }) 
                        }
                    }
                }
                item { SectionHeader(title = "Việc cần làm", action = "Thêm", onActionClick = onAddTodo) }
                items(items = todos, key = { it.id }) { todo -> 
                    TodoSwipeableItem(
                        todo = todo,
                        onToggle = { onToggleTodo(todo.id, it) },
                        onClick = { onEditTodo(todo) },
                        onDelete = { onDeleteTodo(todo) }
                    )
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
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val anchorWidth = with(density) { 70.dp.toPx() }
    val dismissThreshold = with(density) { 180.dp.toPx() }
    
    // Sử dụng Animatable để kiểm soát chuỗi hiệu ứng trượt đi
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        // Nền đỏ và icon thùng rác (Được lộ ra phía sau)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Red)
                .clickable { 
                    scope.launch {
                        // Trượt biến mất hoàn toàn rồi mới xóa
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

        // Nội dung Todo (Lớp phía trên có thể kéo)
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
                                    // Hiệu ứng trượt hẳn ra ngoài màn hình
                                    offsetX.animateTo(-1500f, spring(stiffness = Spring.StiffnessMedium))
                                    onDelete()
                                } else if (offsetX.value < -anchorWidth / 2) {
                                    // Dừng lại ở vị trí lộ nút xóa
                                    offsetX.animateTo(-anchorWidth, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                } else {
                                    // Trở về vị trí ban đầu
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
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..10 -> R.string.home_hello_morning
            in 11..13 -> R.string.home_hello_noon
            in 14..17 -> R.string.home_hello_afternoon
            else -> R.string.home_hello_evening
        }
        Text(text = stringResource(id = greetingRes, userName), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Hôm nay bạn thấy thế nào?", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("😊", "😐", "😔", "😫", "🔥").forEach { mood ->
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = { }) {
                    Box(contentAlignment = Alignment.Center) { Text(mood, fontSize = 20.sp) }
                }
            }
        }
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
private fun InstagramHabitItem(habit: Habit, onToggle: (Boolean) -> Unit) {
    val color = try { Color(habit.colorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onToggle(!habit.isCompleted) }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (habit.isCompleted) color else color.copy(alpha = 0.1f))
                .border(2.dp, if (habit.isCompleted) color else color.copy(alpha = 0.3f), CircleShape)
        ) {
            Text(habit.icon, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = habit.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                0.5.dp, 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), 
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mép bên trái dải màu ưu tiên
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .background(priorityColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                text = todo.task, 
                fontSize = 14.sp,
                textDecoration = if (todo.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null, 
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
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
    onConfirm: (String, Priority) -> Unit
) {
    var task by remember { mutableStateOf(todo?.task ?: "") }
    var priority by remember { mutableStateOf(todo?.priority ?: Priority.MEDIUM) }
    val focusRequester = remember { FocusRequester() }

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

                    OutlinedTextField(
                        value = task,
                        onValueChange = { task = it },
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
                            onClick = { if (task.isNotBlank()) onConfirm(task, priority) },
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

@AppCombinedPreviews
@Composable
fun HomeScreenPreview() {
    NewStartTheme {
        HomeContent(
            user = User(id = "1", name = "Trọng", email = "trong@example.com"),
            habits = emptyList(),
            todos = emptyList(),
            onToggleHabit = { _, _ -> },
            onToggleTodo = { _, _ -> },
            onAddTodo = {},
            onEditTodo = {},
            onDeleteTodo = {},
            timerSeconds = 0,
            isTimerRunning = false,
            onStopTimer = {}
        )
    }
}
