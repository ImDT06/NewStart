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

    private suspend fun fetchHabitsForToday(): List<Pair<String, String>> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val snapshot = FirebaseFirestore.getInstance()
                .collection("habits")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .get()
                .await()
            
            snapshot.documents.map { 
                val name = it.getString("name") ?: ""
                val icon = it.getString("icon") ?: "✨"
                icon to name 
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Composable
    private fun HabitWidgetContent(habits: List<Pair<String, String>>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                text = "Thói quen hôm nay",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ColorProvider(Color(0xFF1D5FE2))
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (habits.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Chưa có thói quen nào",
                        style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray))
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(habits) { habit ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = habit.first, style = TextStyle(fontSize = 14.sp))
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = habit.second,
                                style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.Black)),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
