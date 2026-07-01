package com.example.newstart.data.local.dao

import androidx.room.*
import com.example.newstart.data.local.FriendshipEntity
import com.example.newstart.data.local.SquadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {
    // Friends
    @Query("SELECT * FROM friendships WHERE cachedForUserId = :userId")
    fun getFriends(userId: String): Flow<List<FriendshipEntity>>

    @Query("SELECT * FROM friendships WHERE id = :friendshipId LIMIT 1")
    suspend fun getFriendshipById(friendshipId: String): FriendshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friendships: List<FriendshipEntity>)

    @Query("DELETE FROM friendships WHERE cachedForUserId = :userId")
    suspend fun clearFriends(userId: String)

    @Query("DELETE FROM friendships WHERE id = :friendshipId")
    suspend fun deleteFriend(friendshipId: String)

    // Squads
    @Query("SELECT * FROM squads WHERE cachedForUserId = :userId")
    fun getSquads(userId: String): Flow<List<SquadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSquads(squads: List<SquadEntity>)

    @Query("DELETE FROM squads WHERE cachedForUserId = :userId")
    suspend fun clearSquads(userId: String)
    
    @Query("DELETE FROM squads WHERE id = :squadId")
    suspend fun deleteSquad(squadId: String)
}
