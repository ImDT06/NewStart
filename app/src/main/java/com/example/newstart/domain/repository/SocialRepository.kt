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
    fun getSentRequests(): Flow<List<FriendRequest>>
    suspend fun acceptFriendRequest(requestId: String)
    
    // Squads
    fun getSquads(): Flow<List<Squad>>
    suspend fun createSquad(squad: Squad)
    suspend fun joinSquad(squadId: String)
    suspend fun leaveSquad(squadId: String)
    suspend fun updateSquad(squadId: String, name: String, description: String)
    suspend fun addMemberToSquad(squadId: String, memberId: String)
    suspend fun removeMemberFromSquad(squadId: String, memberId: String)
    fun getSquadMessages(squadId: String): Flow<List<com.example.newstart.domain.model.SquadMessage>>
    suspend fun sendSquadMessage(squadId: String, text: String)
    
    // Feed
    fun getSocialFeed(): Flow<List<JournalEntry>>
    suspend fun refreshSocialFeed()
    suspend fun reactToPost(postId: String, emoji: String)
    suspend fun removeFriend(friendshipId: String)
    suspend fun declineFriendRequest(requestId: String)
    
    // Direct Messages
    fun getDirectMessages(friendshipId: String): Flow<List<com.example.newstart.domain.model.DirectMessage>>
    suspend fun sendDirectMessage(friendshipId: String, text: String, sharedJournal: JournalEntry? = null): Result<Unit>
    fun getLastMessage(friendshipId: String): Flow<com.example.newstart.domain.model.DirectMessage?>
}
