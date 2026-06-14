package com.example.newstart.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.*

/**
 * Lớp dữ liệu Habit. 
 * Đã chuyển isCompleted sang 'val' để đảm bảo tính Immutable (Bất biến),
 * giúp Jetpack Compose tối ưu hóa việc vẽ lại màn hình (Skip recomposition).
 */
@Immutable
@IgnoreExtraProperties
data class Habit(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val icon: String = "✨",
    val progress: String = "0",
    val goal: String = "1",
    val streak: Int = 0,
    val colorHex: String = "#1D5FE2",
    
    @get:PropertyName("isCompleted")
    val isCompleted: Boolean = false,

    val date: String = "", // Định dạng "yyyy-MM-dd"
    val reminderTime: String? = null, // Định dạng "HH:mm"
    val reminderMinutesBefore: Int = 0,
    val createdAt: Date = Date()
)
