package com.example.newstart.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.newstart.domain.model.Habit
import com.example.newstart.receiver.HabitReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object HabitReminderManager {

    fun scheduleReminder(context: Context, habit: Habit) {
        if (habit.reminderTime == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val timeParts = habit.reminderTime.split(":")
        if (timeParts.size != 2) return
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val habitDate = LocalDate.parse(habit.date)
        
        // 1. Nhắc nhở đúng giờ
        val onTimeDateTime = LocalDateTime.of(habitDate, LocalTime.of(hour, minute))
        val onTimeTrigger = onTimeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        if (onTimeTrigger > System.currentTimeMillis()) {
            val intentOnTime = Intent(context, HabitReminderReceiver::class.java).apply {
                putExtra("habitName", habit.name)
                putExtra("habitId", habit.id)
                putExtra("isEarlyReminder", false)
            }
            val pendingOnTime = PendingIntent.getBroadcast(
                context,
                habit.id.hashCode(),
                intentOnTime,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, onTimeTrigger, pendingOnTime)
        }

        // 2. Nhắc nhở trước (nếu có)
        if (habit.reminderMinutesBefore > 0) {
            val earlyDateTime = onTimeDateTime.minusMinutes(habit.reminderMinutesBefore.toLong())
            val earlyTrigger = earlyDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (earlyTrigger > System.currentTimeMillis()) {
                val intentEarly = Intent(context, HabitReminderReceiver::class.java).apply {
                    putExtra("habitName", habit.name)
                    putExtra("habitId", habit.id)
                    putExtra("isEarlyReminder", true)
                    putExtra("minutesBefore", habit.reminderMinutesBefore)
                }
                val pendingEarly = PendingIntent.getBroadcast(
                    context,
                    habit.id.hashCode() + 1, // ID khác để không ghi đè
                    intentEarly,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earlyTrigger, pendingEarly)
            }
        }
    }

    fun cancelReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java)
        
        // Hủy cả 2 báo thức
        val p1 = PendingIntent.getBroadcast(context, habitId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        p1?.let { alarmManager.cancel(it) }
        
        val p2 = PendingIntent.getBroadcast(context, habitId.hashCode() + 1, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        p2?.let { alarmManager.cancel(it) }
    }
}
