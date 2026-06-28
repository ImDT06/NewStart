package com.example.newstart.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.example.newstart.R
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.newstart.MainActivity
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.model.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import com.example.newstart.data.local.toDomain
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitWidget : GlanceAppWidget() {
    companion object {
        val habitIdKey = ActionParameters.Key<String>("habitId")
        val todoIdKey = ActionParameters.Key<String>("todoId")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habits = fetchHabitsForToday(context)
        val todos = fetchTodos(context)
        val overallStreak = calculateOverallStreak(context)
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isStreakEnabled = sharedPrefs.getBoolean("is_streak_widget_enabled", true)
        
        provideContent {
            HabitWidgetContent(habits, todos, overallStreak, isStreakEnabled)
        }
    }

    private fun getDatabase(context: Context): com.example.newstart.data.local.NewStartDatabase {
        return androidx.room.Room.databaseBuilder(
            context.applicationContext,
            com.example.newstart.data.local.NewStartDatabase::class.java,
            com.example.newstart.data.local.NewStartDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
         .enableMultiInstanceInvalidation()
         .build()
    }

    private suspend fun fetchHabitsForToday(context: Context): List<com.example.newstart.domain.model.Habit> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val db = getDatabase(context)
        return try {
            val entities = db.habitDao().getHabitsSync(userId, today)
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("HabitWidget", "Error fetching habits: ${e.message}", e)
            emptyList()
        } finally {
            db.close()
        }
    }

    private suspend fun fetchTodos(context: Context): List<com.example.newstart.domain.model.Todo> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val db = getDatabase(context)
        return try {
            val entities = db.todoDao().getAllTodosSync()
            entities.map { it.toDomain() }.filter { !it.isCompleted && it.userId == userId }
        } catch (e: Exception) {
            android.util.Log.e("HabitWidget", "Error fetching todos: ${e.message}", e)
            emptyList()
        } finally {
            db.close()
        }
    }

    private suspend fun calculateOverallStreak(context: Context): Int {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0
        val db = getDatabase(context)
        return try {
            val entities = db.habitDao().getAllHabitsSync(userId)
            val allHabits = entities.map { it.toDomain() }
            
            val completedDates = allHabits.filter { it.isCompleted }
                .map { it.date }
                .filter { it.isNotBlank() }
                .distinct()
                .mapNotNull { 
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedDescending()

            var streak = 0
            if (completedDates.isNotEmpty()) {
                var current = LocalDate.now()
                if (!completedDates.contains(current)) {
                    current = current.minusDays(1)
                }
                for (date in completedDates) {
                    if (date == current) {
                        streak++
                        current = current.minusDays(1)
                    } else if (date.isBefore(current)) {
                        break
                    }
                }
            }
            streak
        } catch (e: Exception) {
            android.util.Log.e("HabitWidget", "Error calculating overall streak: ${e.message}", e)
            0
        } finally {
            db.close()
        }
    }

    @Composable
    private fun HabitWidgetContent(
        habits: List<com.example.newstart.domain.model.Habit>,
        todos: List<com.example.newstart.domain.model.Todo>,
        overallStreak: Int,
        isStreakEnabled: Boolean
    ) {
        val totalUncompleted = habits.count { !it.isCompleted } + todos.count { !it.isCompleted }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = totalUncompleted.toString(),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = ColorProvider(Color(0xFF1D5FE2))
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Việc cần làm",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ColorProvider(Color(0xFF1D5FE2))
                        )
                    )
                }
                
                if (isStreakEnabled && overallStreak > 0) {
                    StreakBadge(streak = overallStreak)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .background(Color(0xFF1D5FE2))
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (habits.isEmpty() && todos.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Hôm nay thảnh thơi! 🎉",
                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color.Gray))
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    if (habits.isNotEmpty()) {
                        item {
                            Text(
                                text = "Thói quen",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ColorProvider(Color.Gray)
                                ),
                                modifier = GlanceModifier.padding(vertical = 4.dp)
                            )
                        }
                        items(habits.sortedBy { it.reminderTime ?: "23:59" }) { habit ->

                            
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 6.dp)
                                    .background(if (habit.isCompleted) Color(0xFFF2F7FF) else Color.Transparent)
                                    .cornerRadius(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(32.dp)
                                        .clickable(
                                            actionRunCallback<ToggleHabitAction>(
                                                actionParametersOf(
                                                    HabitWidget.habitIdKey to habit.id
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        provider = ImageProvider(
                                            if (habit.isCompleted) R.drawable.ic_checkbox_checked 
                                            else R.drawable.ic_checkbox_unchecked
                                        ),
                                        contentDescription = "Toggle Habit",
                                        modifier = GlanceModifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Row(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .clickable(actionStartActivity<MainActivity>()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = habit.icon,
                                        style = TextStyle(fontSize = 16.sp)
                                    )
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = habit.name,
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ColorProvider(if (habit.isCompleted) Color.Gray else Color.Black)
                                            ),
                                            maxLines = 1
                                        )
                                        if (habit.reminderTime != null) {
                                            Text(
                                                text = "⏰ ${habit.reminderTime}",
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    color = ColorProvider(Color(0xFF1D5FE2))
                                                )
                                            )
                                        }
                                    }
                                    if (isStreakEnabled && habit.streak > 0) {
                                        StreakBadge(streak = habit.streak)
                                    }
                                }
                            }
                        }
                    }

                    if (todos.isNotEmpty()) {
                        item {
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Text(
                                text = "Nhiệm vụ cần làm",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ColorProvider(Color.Gray)
                                ),
                                modifier = GlanceModifier.padding(vertical = 4.dp)
                            )
                        }
                        items(todos) { todo ->

                            val priorityDot = when (todo.priority) {
                                com.example.newstart.domain.model.Priority.HIGH -> "🔴"
                                com.example.newstart.domain.model.Priority.MEDIUM -> "🟡"
                                com.example.newstart.domain.model.Priority.LOW -> "🟢"
                            }
                            
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 6.dp)
                                    .background(if (todo.isCompleted) Color(0xFFF2F7FF) else Color.Transparent)
                                    .cornerRadius(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(32.dp)
                                        .clickable(
                                            actionRunCallback<ToggleTodoAction>(
                                                actionParametersOf(
                                                    HabitWidget.todoIdKey to todo.id
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        provider = ImageProvider(
                                            if (todo.isCompleted) R.drawable.ic_checkbox_checked 
                                            else R.drawable.ic_checkbox_unchecked
                                        ),
                                        contentDescription = "Toggle Todo",
                                        modifier = GlanceModifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Row(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .clickable(actionStartActivity<MainActivity>()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = priorityDot,
                                        style = TextStyle(fontSize = 14.sp)
                                    )
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = todo.task,
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ColorProvider(if (todo.isCompleted) Color.Gray else Color.Black)
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StreakBadge(streak: Int) {
        Row(
            modifier = GlanceModifier
                .background(Color(0xFFE8F0FE))
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥",
                style = TextStyle(fontSize = 11.sp)
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = streak.toString(),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF1D5FE2))
                )
            )
        }
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitWidget.habitIdKey] ?: return

        withContext(Dispatchers.IO) {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    com.example.newstart.data.local.NewStartDatabase::class.java,
                    com.example.newstart.data.local.NewStartDatabase.DATABASE_NAME
                ).fallbackToDestructiveMigration().enableMultiInstanceInvalidation().build()
                
                var targetIsCompleted = false
                try {
                    val habit = db.habitDao().getAllHabitsSync(FirebaseAuth.getInstance().currentUser?.uid ?: "").find { it.id == habitId }
                    if (habit != null) {
                        targetIsCompleted = !habit.isCompleted
                        db.habitDao().updateCompletion(habitId, targetIsCompleted, isSynced = false)
                    } else {
                        // Fallback if not found in sync list, just default to true
                        targetIsCompleted = true
                        db.habitDao().updateCompletion(habitId, true, isSynced = false)
                    }
                } finally {
                    db.close()
                }

                HabitWidget().updateAll(context)

                try {
                    FirebaseFirestore.getInstance()
                        .collection("habits")
                        .document(habitId)
                        .update("isCompleted", targetIsCompleted)
                        .await()
                    
                    val db2 = androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        com.example.newstart.data.local.NewStartDatabase::class.java,
                        com.example.newstart.data.local.NewStartDatabase.DATABASE_NAME
                    ).fallbackToDestructiveMigration().enableMultiInstanceInvalidation().build()
                    try {
                        db2.habitDao().updateCompletion(habitId, targetIsCompleted, isSynced = true)
                    } finally {
                        db2.close()
                    }
                } catch (e: Exception) {
                    // Ignore remote network error for offline support
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitWidget", "Error updating habit: ${e.message}")
            }
        }
    }
}

class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters[HabitWidget.todoIdKey] ?: return

        withContext(Dispatchers.IO) {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    com.example.newstart.data.local.NewStartDatabase::class.java,
                    com.example.newstart.data.local.NewStartDatabase.DATABASE_NAME
                ).fallbackToDestructiveMigration().enableMultiInstanceInvalidation().build()
                
                var targetIsCompleted = false
                try {
                    val todo = db.todoDao().getTodoById(todoId)
                    if (todo != null) {
                        targetIsCompleted = !todo.isCompleted
                        db.todoDao().toggleTodoCompletion(todoId, targetIsCompleted)
                    } else {
                        targetIsCompleted = true
                        db.todoDao().toggleTodoCompletion(todoId, true)
                    }
                } finally {
                    db.close()
                }

                HabitWidget().updateAll(context)

                try {
                    FirebaseFirestore.getInstance()
                        .collection("todos")
                        .document(todoId)
                        .update("isCompleted", targetIsCompleted)
                        .await()
                } catch (e: Exception) {
                    // Ignore remote network error for offline support
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitWidget", "Error updating todo: ${e.message}")
            }
        }
    }
}
