package com.example.newstart.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.newstart.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habits = fetchHabitsForToday()
        
        provideContent {
            HabitWidgetContent(habits)
        }
    }

    private suspend fun fetchHabitsForToday(): List<com.example.newstart.domain.model.Habit> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val snapshot = FirebaseFirestore.getInstance()
                .collection("habits")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .get()
                .await()
            
            snapshot.toObjects(com.example.newstart.domain.model.Habit::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Composable
    private fun HabitWidgetContent(habits: List<com.example.newstart.domain.model.Habit>) {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM", java.util.Locale("vi"))
        val dateString = today.format(dateFormatter)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateString,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(Color.Gray)
                        )
                    )
                    Text(
                        text = "Thói quen hôm nay",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ColorProvider(Color(0xFF1D5FE2))
                        )
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.height(12.dp))

            if (habits.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Chưa có thói quen nào",
                        style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.Gray))
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(habits.sortedBy { it.reminderTime ?: "23:59" }) { habit ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(if (habit.isCompleted) Color(0xFFF0F7FF) else Color.Transparent),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = habit.icon,
                                style = TextStyle(fontSize = 18.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = habit.name,
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorProvider(if (habit.isCompleted) Color.Gray else Color.Black)
                                    ),
                                    maxLines = 1
                                )
                                if (habit.reminderTime != null) {
                                    Text(
                                        text = "⏰ ${habit.reminderTime}",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = ColorProvider(Color(0xFF1D5FE2))
                                        )
                                    )
                                }
                            }
                            if (habit.isCompleted) {
                                Text(
                                    text = "✅",
                                    style = TextStyle(fontSize = 14.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
