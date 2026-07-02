package com.example.newstart.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.glance.appwidget.updateAll

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val itemId = intent.getStringExtra("item_id") ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    com.example.newstart.data.local.NewStartDatabase::class.java,
                    com.example.newstart.data.local.NewStartDatabase.DATABASE_NAME
                ).fallbackToDestructiveMigration().enableMultiInstanceInvalidation().build()

                val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val userId = FirebaseAuth.getInstance().currentUser?.uid 
                    ?: sharedPrefs.getString("logged_in_user_id", "") 
                    ?: ""

                if (action == "com.example.newstart.ACTION_TOGGLE_HABIT") {
                    var targetIsCompleted = false
                    try {
                        val habit = db.habitDao().getAllHabitsSync(userId).find { it.id == itemId }
                        if (habit != null) {
                            targetIsCompleted = !habit.isCompleted
                            db.habitDao().updateCompletion(itemId, targetIsCompleted, isSynced = false)
                        } else {
                            targetIsCompleted = true
                            db.habitDao().updateCompletion(itemId, true, isSynced = false)
                        }
                    } finally {
                        db.close()
                    }

                    HabitWidget().updateAll(context)

                    try {
                        FirebaseFirestore.getInstance()
                            .collection("habits")
                            .document(itemId)
                            .update("isCompleted", targetIsCompleted)
                            .await()
                    } catch (e: Exception) {
                        // ignore
                    }
                } else if (action == "com.example.newstart.ACTION_TOGGLE_TODO") {
                    var targetIsCompleted = false
                    try {
                        val todo = db.todoDao().getTodoById(itemId)
                        if (todo != null) {
                            targetIsCompleted = !todo.isCompleted
                            db.todoDao().toggleTodoCompletion(itemId, targetIsCompleted)
                        } else {
                            targetIsCompleted = true
                            db.todoDao().toggleTodoCompletion(itemId, true)
                        }
                    } finally {
                        db.close()
                    }

                    HabitWidget().updateAll(context)

                    try {
                        FirebaseFirestore.getInstance()
                            .collection("todos")
                            .document(itemId)
                            .update("isCompleted", targetIsCompleted)
                            .await()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WidgetActionReceiver", "Error: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
