package com.example.newstart.ui.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val habits by viewModel.todayHabits.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()

    HomeContent(
        user = userState,
        habits = habits,
        todos = todos,
        onToggleHabit = { h, c -> viewModel.toggleHabit(h, c) },
        onToggleTodo = { id, c -> viewModel.toggleTodo(id, c) },
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    user: User?,
    habits: List<Habit>,
    todos: List<Todo>,
    onToggleHabit: (Habit, Boolean) -> Unit,
    onToggleTodo: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                item { HomeHeaderSection(userName = user?.name ?: "Guest") }
                item { DailyOverviewCard(habits, todos) }
                item { SectionHeader(title = "Thói quen hôm nay", action = "Tất cả")
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items = habits, key = { it.id }) { habit -> 
                            HomeHabitItem(habit, onToggle = { onToggleHabit(habit, it) }) 
                        }
                    }
                }
                item { SectionHeader(title = "Việc cần làm", action = "Thêm") }
                items(items = todos, key = { it.id }) { todo -> 
                    HomeTodoItem(todo, onToggle = { onToggleTodo(todo.id, it) }) 
                }
                item { AIInsightCard() }
            }
        }
    }
}

@Composable
private fun HomeHeaderSection(userName: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..10 -> R.string.home_hello_morning
            in 11..13 -> R.string.home_hello_noon
            in 14..17 -> R.string.home_hello_afternoon
            else -> R.string.home_hello_evening
        }
        Text(text = stringResource(id = greetingRes, userName), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Hôm nay bạn thấy thế nào?", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("😊", "😐", "😔", "😫", "🔥").forEach { mood ->
                Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = { }) {
                    Box(contentAlignment = Alignment.Center) { Text(mood, fontSize = 24.sp) }
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

    Card(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp, trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                Text("$percent%", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = "Tiến độ ngày", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Bạn đã hoàn thành $completed/$total mục tiêu. Cố gắng lên!", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun HomeHabitItem(habit: Habit, onToggle: (Boolean) -> Unit) {
    val color = try { Color(habit.colorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    Surface(
        modifier = Modifier.width(100.dp).clickable { onToggle(!habit.isCompleted) }, 
        shape = RoundedCornerShape(24.dp), 
        color = if (habit.isCompleted) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(habit.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = habit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (habit.isCompleted) Color.White else MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun HomeTodoItem(todo: Todo, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = todo.isCompleted, onCheckedChange = onToggle, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = todo.task, modifier = Modifier.weight(1f), textDecoration = if (todo.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null, color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface)
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(when (todo.priority) { Priority.HIGH -> Color(0xFFFF4444); Priority.MEDIUM -> Color(0xFFFFBB33); Priority.LOW -> Color(0xFF00C851) }))
    }
}

@Composable
private fun AIInsightCard() {
    val aiGradient = Brush.linearGradient(colors = listOf(Color(0xFF4285F4), Color(0xFF9B72CB)))
    Card(modifier = Modifier.fillMaxWidth().padding(20.dp).border(1.dp, aiGradient, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF9B72CB))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "AI Gợi ý", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(brush = aiGradient))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Bạn thường làm việc hiệu quả nhất vào 9h sáng. Hãy sắp xếp các việc HIGH priority vào khung giờ này nhé!", fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = action, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
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
            onToggleTodo = { _, _ -> }
        )
    }
}
