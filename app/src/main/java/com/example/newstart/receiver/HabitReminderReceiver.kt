package com.example.newstart.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.newstart.MainActivity
import com.example.newstart.R

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        val habitId = intent.getStringExtra("habitId") ?: ""
        val isEarlyReminder = intent.getBooleanExtra("isEarlyReminder", false)
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        
        val title = if (isEarlyReminder) "Sắp đến giờ rồi!" else "Đã đến lúc!"
        val message = if (isEarlyReminder) {
            "Chỉ còn $minutesBefore phút nữa là đến giờ: $habitName"
        } else {
            "Đã đến lúc thực hiện thói quen: $habitName"
        }

        showNotification(context, title, message, habitId, isEarlyReminder)
    }

    private fun showNotification(
        context: Context, 
        title: String, 
        message: String, 
        habitId: String,
        isEarly: Boolean
    ) {
        val channelId = "habit_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // ... channel setup unchanged ...

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for habit reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (isEarly) habitId.hashCode() + 1 else habitId.hashCode()
        notificationManager.notify(notificationId, notification)
    }
}
