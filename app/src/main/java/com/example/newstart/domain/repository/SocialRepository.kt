package com.example.newstart.domain.repository

import com.example.newstart.domain.model.FriendRequest
import com.example.newstart.domain.model.Friendship
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.Squad
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    // Friends
    fun getFriends(): Flow<List<Friendship>>
    suspend fun sendFriendRequest(toUserId: String)
    fun getIncomingRequests(): Flow<List<FriendRequest>>
    suspend fun acceptFriendRequest(requestId: String)
    
    // Squads
    fun getSquads(): Flow<List<Squad>>
    suspend fun joinSquad(squadId: String)
    
    // Feed
    fun getSocialFeed(): Flow<List<JournalEntry>>
    suspend fun reactToPost(postId: String, emoji: String)
}
