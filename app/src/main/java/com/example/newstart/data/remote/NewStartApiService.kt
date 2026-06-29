package com.example.newstart.data.remote

import com.example.newstart.data.remote.dto.HabitDto
import com.example.newstart.data.remote.dto.TodoDto
import retrofit2.http.*

interface NewStartApiService {
    // Habits
    @GET("api/habits")
    suspend fun getHabits(@Query("date") date: String): List<HabitDto>

    @POST("api/habits")
    suspend fun createHabit(@Body habit: HabitDto): HabitDto

    // Todos
    @GET("api/todos")
    suspend fun getTodos(): List<TodoDto>

    @POST("api/todos")
    suspend fun createTodo(@Body todo: TodoDto): TodoDto

    @PUT("api/todos/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body todo: TodoDto): TodoDto

    @DELETE("api/todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String)

    // Journal
    @GET("api/journal")
    suspend fun getJournalEntries(): List<Map<String, @kotlinx.serialization.Contextual Any>>

    @POST("api/journal")
    suspend fun createJournalEntry(@Body entry: Map<String, @kotlinx.serialization.Contextual Any>): Map<String, @kotlinx.serialization.Contextual Any>

    @DELETE("api/journal/{id}")
    suspend fun deleteJournalEntry(@Path("id") id: String)

    // Social
    @GET("api/social/feed")
    suspend fun getSocialFeed(): List<Map<String, @kotlinx.serialization.Contextual Any>>

    @POST("api/social/react/{postId}")
    suspend fun reactToPost(
        @Path("postId") postId: String,
        @Query("emoji") emoji: String
    )
}
