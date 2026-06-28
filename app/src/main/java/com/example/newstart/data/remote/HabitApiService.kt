package com.example.newstart.data.remote

import com.example.newstart.data.remote.dto.HabitDto
import retrofit2.http.*

interface HabitApiService {
    @GET("api/habits")
    suspend fun getHabits(
        @Query("date") date: String
    ): List<HabitDto>

    @POST("api/habits")
    suspend fun createHabit(
        @Body habit: HabitDto
    ): HabitDto
}
