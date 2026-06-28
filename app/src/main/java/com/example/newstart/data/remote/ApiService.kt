package com.example.newstart.data.remote

import com.example.newstart.domain.model.*
import retrofit2.http.*

interface ApiService {

    // --- Authentication ---
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): User

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): User

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: Map<String, String>): User

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>)

    @POST("auth/send-verification")
    suspend fun sendVerification()

    // --- User Profile ---
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): User

    @PUT("users/{id}/avatar")
    suspend fun updateAvatar(@Path("id") id: String, @Body body: Map<String, String>): User

    @PUT("users/{id}/profile")
    suspend fun updateProfile(@Path("id") id: String, @Body body: Map<String, String>): User

    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): List<User>

    // --- Habits ---
    @GET("habits")
    suspend fun getHabits(
        @Query("date") date: String?,
        @Query("userId") userId: String
    ): List<Habit>

    @GET("habits/all")
    suspend fun getAllHabits(@Query("userId") userId: String): List<Habit>

    @POST("habits")
    suspend fun saveHabit(@Body habit: Habit): Habit

    @PUT("habits/{id}")
    suspend fun updateHabit(@Path("id") id: String, @Body habit: Habit): Habit

    @DELETE("habits/{id}")
    suspend fun deleteHabit(@Path("id") id: String)

    @PUT("habits/{id}/toggle")
    suspend fun toggleHabitCompletion(@Path("id") id: String, @Body body: Map<String, Boolean>)

    // --- Journal ---
    @GET("journals")
    suspend fun getJournals(@Query("userId") userId: String): List<JournalEntry>

    @POST("journals")
    suspend fun saveJournal(@Body entry: JournalEntry): JournalEntry

    @DELETE("journals/{id}")
    suspend fun deleteJournal(@Path("id") id: String)

    // --- Todos ---
    @GET("todos")
    suspend fun getTodos(@Query("userId") userId: String): List<Todo>

    @POST("todos")
    suspend fun saveTodo(@Body todo: Todo): Todo

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body todo: Todo): Todo

    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String)

    @PUT("todos/{id}/toggle")
    suspend fun toggleTodoCompletion(@Path("id") id: String, @Body body: Map<String, Boolean>)

    // --- Social / Friends ---
    @GET("social/friends")
    suspend fun getFriends(@Query("userId") userId: String): List<Friendship>

    @POST("social/friend-requests")
    suspend fun sendFriendRequest(@Body body: Map<String, String>)

    @GET("social/incoming-requests")
    suspend fun getIncomingRequests(@Query("userId") userId: String): List<FriendRequest>

    @GET("social/sent-requests")
    suspend fun getSentRequests(@Query("userId") userId: String): List<FriendRequest>

    @POST("social/friend-requests/{id}/accept")
    suspend fun acceptFriendRequest(@Path("id") id: String)

    @POST("social/friend-requests/{id}/decline")
    suspend fun declineFriendRequest(@Path("id") id: String)

    @DELETE("social/friends/{id}")
    suspend fun removeFriend(@Path("id") id: String)

    @GET("social/feed")
    suspend fun getSocialFeed(@Query("userId") userId: String): List<JournalEntry>

    @POST("social/posts/{id}/react")
    suspend fun reactToPost(@Path("id") id: String, @Body body: Map<String, String>)

    // --- Squads ---
    @GET("squads")
    suspend fun getSquads(@Query("userId") userId: String): List<Squad>

    @POST("squads")
    suspend fun createSquad(@Body squad: Squad): Squad

    @POST("squads/{id}/join")
    suspend fun joinSquad(@Path("id") id: String, @Body body: Map<String, String>)

    @POST("squads/{id}/leave")
    suspend fun leaveSquad(@Path("id") id: String, @Body body: Map<String, String>)

    @PUT("squads/{id}")
    suspend fun updateSquad(@Path("id") id: String, @Body body: Map<String, String>)

    @POST("squads/{id}/members/add")
    suspend fun addMemberToSquad(@Path("id") id: String, @Body body: Map<String, String>)

    @POST("squads/{id}/members/remove")
    suspend fun removeMemberFromSquad(@Path("id") id: String, @Body body: Map<String, String>)

    @GET("squads/{id}/messages")
    suspend fun getSquadMessages(@Path("id") id: String): List<SquadMessage>

    @POST("squads/{id}/messages")
    suspend fun sendSquadMessage(@Path("id") id: String, @Body body: Map<String, String>)
}
