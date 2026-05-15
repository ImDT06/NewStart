package com.example.newstart.ui.screens.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

data class Habit(
    val id: String,
    val name: String,
    val icon: String,
    val progress: String,
    val goal: String,
    val streak: Int? = null,
    val color: Color,
    val isCompleted: Boolean = false
)

@Composable
fun HabitsScreen(modifier: Modifier = Modifier) {
    val habits = listOf(
        Habit("1", "Zhongwen", "🇨🇳", "47m 44s", "1h", null, Color(0xFFFF3B30), true),
        Habit("2", "Read a book", "📚", "0", "1h", 1, Color(0xFF1D1D1F)),
        Habit("3", "Drink water", "💧", "0", "3000 ml", null, Color(0xFF1D1D1F))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Luôn để nền đen theo mẫu
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
                text = "Today",
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

        // Horizontal Date Picker
        val today = LocalDate.now()
        val days = (0..6).map { today.plusDays(it.toLong() - 3) }
        
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(days) { day ->
                val isSelected = day == today
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFFF4D67)) else null,
                        onClick = { /* Select date */ }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
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
                HabitItem(habit)
            }
        }
    }
}

@Composable
fun HabitItem(habit: Habit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = habit.color.copy(alpha = if (habit.isCompleted) 1f else 0.12f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(32.dp),
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
                    if (habit.streak != null) {
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
                onClick = { /* Action */ },
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

@Preview(showBackground = true)
@Composable
fun HabitsScreenPreview() {
    NewStartTheme {
        HabitsScreen()
    }
}
