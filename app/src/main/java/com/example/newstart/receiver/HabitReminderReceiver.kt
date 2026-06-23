package com.example.newstart.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.newstart.MainActivity
import com.example.newstart.R

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("habitName") ?: "Thói quen"
        val habitId = intent.getStringExtra("habitId") ?: ""
        val isEarlyReminder = intent.getBooleanExtra("isEarlyReminder", false)
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        
        Log.d("HabitReminderReceiver", "Nhận được tín hiệu báo thức cho: $habitName (isEarly: $isEarlyReminder)")

        val quotes = listOf(
            "Hành trình ngàn dặm bắt đầu từ một bước chân.",
            "Kỷ luật là cầu nối giữa mục tiêu và thành tựu.",
            "Đừng dừng lại cho đến khi bạn tự hào.",
            "Thành công không phải là ngẫu nhiên, đó là sự lựa chọn.",
            "Mỗi ngày là một cơ hội mới để trở nên tốt hơn.",
            "Bí mật của sự thành công là bắt đầu.",
            "Sự kiên trì chính là chìa khóa của mọi cánh cửa.",
            "Hãy làm hôm nay để ngày mai bạn phải cảm ơn chính mình."
        )
        val randomQuote = quotes.random()

        val title = if (isEarlyReminder) "Sắp đến giờ rồi! ✨" else "Đã đến lúc! 🚀"
        val message = if (isEarlyReminder) {
            "Chỉ còn $minutesBefore phút: $habitName. $randomQuote"
        } else {
            "Thực hiện ngay: $habitName. $randomQuote"
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở thói quen"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode() + (if (isEarly) 1 else 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Đảm bảo icon này tồn tại
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (isEarly) habitId.hashCode() + 1 else habitId.hashCode()
        notificationManager.notify(notificationId, notification)
        Log.d("HabitReminderReceiver", "Đã gửi thông báo ra hệ thống (ID: $notificationId)")
    }
}
