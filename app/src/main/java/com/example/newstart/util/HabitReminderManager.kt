package com.example.newstart.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.newstart.domain.model.Habit
import com.example.newstart.receiver.HabitReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object HabitReminderManager {
    private const val TAG = "HabitReminderManager"

    fun scheduleReminder(context: Context, habit: Habit) {
        if (habit.reminderTime == null) {
            Log.d(TAG, "Habit ${habit.name} không có giờ nhắc nhở.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            val timeParts = habit.reminderTime.split(":")
            if (timeParts.size != 2) return
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val habitDate = LocalDate.parse(habit.date)
            
            val onTimeDateTime = LocalDateTime.of(habitDate, LocalTime.of(hour, minute))
            val onTimeTrigger = onTimeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            Log.d(TAG, "Đang đặt báo thức cho ${habit.name} tại $onTimeDateTime (Trigger: $onTimeTrigger)")

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
                
                // Sử dụng fallback nếu không có quyền báo thức chính xác
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Không có quyền báo thức chính xác, sử dụng setAndAllowWhileIdle (có thể trễ vài phút)")
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, onTimeTrigger, pendingOnTime)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, onTimeTrigger, pendingOnTime)
                    Log.d(TAG, "Đã đặt báo thức ĐÚNG GIỜ thành công.")
                }
            } else {
                Log.w(TAG, "Thời gian đặt báo thức nằm trong quá khứ, bỏ qua.")
            }

            // Nhắc nhở trước
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
                        habit.id.hashCode() + 1,
                        intentEarly,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earlyTrigger, pendingEarly)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earlyTrigger, pendingEarly)
                    }
                    Log.d(TAG, "Đã đặt báo thức NHẮC TRƯỚC ${habit.reminderMinutesBefore} phút thành công.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đặt báo thức: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java)
        
        val p1 = PendingIntent.getBroadcast(context, habitId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        p1?.let { 
            alarmManager.cancel(it) 
            Log.d(TAG, "Đã hủy báo thức đúng giờ cho $habitId")
        }
        
        val p2 = PendingIntent.getBroadcast(context, habitId.hashCode() + 1, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        p2?.let { 
            alarmManager.cancel(it) 
            Log.d(TAG, "Đã hủy báo thức nhắc sớm cho $habitId")
        }
    }
}
