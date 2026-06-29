package com.example.newstart.data.repository

import com.example.newstart.data.remote.NewStartApiService
import com.example.newstart.data.remote.dto.HabitDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class HabitRemoteRepository @Inject constructor(
    private val apiService: NewStartApiService
) {
    // Hàm lấy thói quen từ Server Spring Boot
    fun getHabitsFromServer(date: String): Flow<List<HabitDto>> = flow {
        try {
            val response = apiService.getHabits(date)
            emit(response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // Hàm gửi thói quen mới lên Server
    suspend fun saveHabitToServer(habit: HabitDto) {
        try {
            apiService.createHabit(habit)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
