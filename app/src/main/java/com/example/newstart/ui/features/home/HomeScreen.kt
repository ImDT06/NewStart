package com.example.newstart.ui.features.home

import androidx.compose.runtime.Immutable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.R
import com.example.newstart.domain.model.User
import com.example.newstart.ui.features.home.HomeViewModel
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews

@Immutable
data class HomeHabit(val id: String, val name: String, val icon: String, val color: Color, val isCompleted: Boolean)
@Immutable
data class HomeTodo(val id: String, val task: String, val isDone: Boolean, val priority: HomePriority)
enum class HomePriority { LOW, MEDIUM, HIGH }

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    HomeContent(user = userState, modifier = modifier)
}

@Composable
fun HomeContent(user: User?, modifier: Modifier = Modifier) {
    val habits = listOf(
        HomeHabit("1", "Uống nước", "💧", Color(0xFF2196F3), true),
        HomeHabit("2", "Đọc sách", "📚", Color(0xFF4CAF50), false),
        HomeHabit("3", "Thiền", "🧘", Color(0xFFFF9800), false),
        HomeHabit("4", "Chạy bộ", "🏃", Color(0xFFE91E63), true)
    )

    val todos = listOf(
        HomeTodo("1", "Hoàn thành UI trang chủ", false, HomePriority.HIGH),
        HomeTodo("2", "Gửi báo cáo đồ án", true, HomePriority.MEDIUM),
        HomeTodo("3", "Mua đồ ăn tối", false, HomePriority.LOW)
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                item { HomeHeaderSection(userName = user?.name ?: "Guest") }
                item { DailyOverviewCard() }
                item { SectionHeader(title = "Thói quen hôm nay", action = "Tất cả")
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items = habits, key = { it.id }) { habit -> HomeHabitItem(habit) }
                    }
                }
                item { SectionHeader(title = "Việc cần làm", action = "Thêm") }
                items(items = todos, key = { it.id }) { todo -> HomeTodoItem(todo) }
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
private fun DailyOverviewCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp, trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                Text("65%", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = "Tiến độ ngày", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Bạn đã hoàn thành 8/12 mục tiêu. Cố gắng lên!", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun HomeHabitItem(habit: HomeHabit) {
    Surface(modifier = Modifier.width(100.dp), shape = RoundedCornerShape(24.dp), color = if (habit.isCompleted) habit.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = BorderStroke(1.dp, habit.color.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(habit.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = habit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (habit.isCompleted) Color.White else MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun HomeTodoItem(todo: HomeTodo) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = todo.isDone, onCheckedChange = {}, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = todo.task, modifier = Modifier.weight(1f), textDecoration = if (todo.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null, color = if (todo.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface)
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(when (todo.priority) { HomePriority.HIGH -> Color(0xFFFF4444); HomePriority.MEDIUM -> Color(0xFFFFBB33); HomePriority.LOW -> Color(0xFF00C851) }))
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
        HomeContent(user = User(id = "1", name = "Trọng", email = "trong@example.com"))
    }
}
