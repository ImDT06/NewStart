package com.example.newstart.data.remote

import com.example.newstart.data.remote.dto.*
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
    suspend fun getJournalEntries(): List<JournalEntryDto>

    @POST("api/journal")
    suspend fun createJournalEntry(@Body entry: JournalEntryDto): JournalEntryDto

    @DELETE("api/journal/{id}")
    suspend fun deleteJournalEntry(@Path("id") id: String)

    // Social
    @GET("api/social/feed")
    suspend fun getSocialFeed(): List<JournalEntryDto>

    @POST("api/social/react/{postId}")
    suspend fun reactToPost(
        @Path("postId") postId: String,
        @Query("emoji") emoji: String
    )

    // Friends
    @GET("api/friends")
    suspend fun getFriends(): List<FriendshipDto>

    @POST("api/friends/request/{toUserId}")
    suspend fun sendFriendRequest(@Path("toUserId") toUserId: String)

    @POST("api/friends/accept/{requestId}")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: String)

    // Squads
    @GET("api/squads")
    suspend fun getSquads(): List<SquadDto>

    @POST("api/squads")
    suspend fun createSquad(@Body squad: SquadDto): SquadDto

    @POST("api/squads/{id}/join")
    suspend fun joinSquad(@Path("id") id: String)

    @POST("api/squads/{id}/leave")
    suspend fun leaveSquad(@Path("id") id: String)

    @GET("api/squads/{id}/messages")
    suspend fun getSquadMessages(@Path("id") id: String): List<SquadMessageDto>

    @POST("api/squads/{id}/messages")
    suspend fun sendSquadMessage(@Path("id") id: String, @Body message: SquadMessageDto)

    // Users
    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserDto

    @GET("api/users/search")
    suspend fun searchUsers(@Query("query") query: String): List<UserDto>

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body updates: Map<String, String>): UserDto
}
