package com.example.newstart.ui.screens.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.theme.NewStartTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*

@Composable
fun HabitsScreen(
    mainViewModel: MainViewModel, // Truyền từ Activity qua NavGraph
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val selectedDate by mainViewModel.selectedHabitDate.collectAsState()
    
    // Sync HabitsViewModel date with MainViewModel date
    LaunchedEffect(selectedDate) {
        viewModel.onDateSelected(selectedDate)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Luôn để nền đen theo mẫu
            .statusBarsPadding()
    ) {
        // ... (Top Bar)
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFFF4D67),
                shape = RoundedCornerShape(12.dp),
                onClick = { /* All filter */ }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }

            Text(
                text = if (selectedDate == LocalDate.now()) "Today" else selectedDate.format(DateTimeFormatter.ofPattern("MMM dd")),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Box(modifier = Modifier.size(26.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color.Yellow,
                    modifier = Modifier.size(22.dp).align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("😊", fontSize = 12.sp)
                    }
                }
            }
        }

        // Horizontal Date Picker with Weekly Paging
        val today = LocalDate.now()
        val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500) // 1000 weeks range

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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { mainViewModel.onHabitDateSelected(day) }
                    ) {
                        Text(
                            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(),
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFFFF4D67) else Color.Transparent,
                            border = if (!isSelected && isToday) BorderStroke(2.dp, Color(0xFFFF4D67)) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    color = Color.White,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(habits) { habit ->
                HabitItem(
                    habit = habit,
                    onToggle = { viewModel.toggleHabit(habit.id, !habit.isCompleted) },
                    onDelete = { viewModel.deleteHabit(habit.id) }
                )
            }
        }
    }
}

@Composable
fun HabitItem(
    habit: Habit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa thói quen") },
            text = { Text("Bạn có chắc chắn muốn xóa '${habit.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    val color = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF1D5FE2)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = if (habit.isCompleted) 1f else 0.12f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { showDeleteDialog = true }, // Nhấn vào icon để xóa
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.1f)
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
                        color = Color.White,
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
                            "${habit.streak} Day",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "${habit.progress}/${habit.goal}",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // Action Button
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (habit.isCompleted) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = if (habit.isCompleted) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/*
@Preview(showBackground = true)
@Composable
fun HabitsScreenPreview() {
    NewStartTheme {
        HabitsScreen()
    }
}
*/
