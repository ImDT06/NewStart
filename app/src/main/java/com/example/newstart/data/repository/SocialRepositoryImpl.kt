package com.example.newstart.data.repository

import com.example.newstart.data.remote.ApiService
import com.example.newstart.domain.model.*
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService
) : SocialRepository {

    override fun getFriends(): Flow<List<Friendship>> = flow {
        val userId = authRepository.currentUserId
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            emit(apiService.getFriends(userId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error fetching friends: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun sendFriendRequest(toUserId: String) {
        val fromUserId = authRepository.currentUserId ?: return
        try {
            apiService.sendFriendRequest(mapOf("fromUserId" to fromUserId, "toUserId" to toUserId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error sending friend request: ${e.message}", e)
        }
    }

    override fun getIncomingRequests(): Flow<List<FriendRequest>> = flow {
        val userId = authRepository.currentUserId
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            emit(apiService.getIncomingRequests(userId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error fetching incoming requests: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getSentRequests(): Flow<List<FriendRequest>> = flow {
        val userId = authRepository.currentUserId
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            emit(apiService.getSentRequests(userId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error fetching sent requests: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun acceptFriendRequest(requestId: String) {
        try {
            apiService.acceptFriendRequest(requestId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error accepting friend request: ${e.message}", e)
        }
    }

    override fun getSquads(): Flow<List<Squad>> = flow {
        val userId = authRepository.currentUserId
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            emit(apiService.getSquads(userId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error fetching squads: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun createSquad(squad: Squad) {
        val userId = authRepository.currentUserId ?: return
        val squadWithAdmin = squad.copy(
            adminId = userId,
            members = (squad.members + userId).distinct()
        )
        try {
            apiService.createSquad(squadWithAdmin)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error creating squad: ${e.message}")
        }
    }

    override suspend fun joinSquad(squadId: String) {
        val userId = authRepository.currentUserId ?: return
        try {
            apiService.joinSquad(squadId, mapOf("userId" to userId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error joining squad: ${e.message}")
        }
    }

    override suspend fun leaveSquad(squadId: String) {
        val userId = authRepository.currentUserId ?: return
        try {
            apiService.leaveSquad(squadId, mapOf("userId" to userId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error leaving squad: ${e.message}")
        }
    }

    override suspend fun updateSquad(squadId: String, name: String, description: String) {
        try {
            apiService.updateSquad(squadId, mapOf("name" to name, "description" to description))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error updating squad: ${e.message}")
        }
    }

    override fun getSocialFeed(): Flow<List<JournalEntry>> = flow {
        val currentUserId = authRepository.currentUserId
        if (currentUserId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            emit(apiService.getSocialFeed(currentUserId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error fetching feed: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun reactToPost(postId: String, emoji: String) {
        val userId = authRepository.currentUserId ?: return
        try {
            apiService.reactToPost(postId, mapOf("userId" to userId, "emoji" to emoji))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error reacting to post: ${e.message}", e)
        }
    }

    override suspend fun removeFriend(friendshipId: String) {
        try {
            apiService.removeFriend(friendshipId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error removing friend: ${e.message}", e)
        }
    }

    override suspend fun declineFriendRequest(requestId: String) {
        try {
            apiService.declineFriendRequest(requestId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error declining request: ${e.message}", e)
        }
    }

    override suspend fun addMemberToSquad(squadId: String, memberId: String) {
        try {
            apiService.addMemberToSquad(squadId, mapOf("memberId" to memberId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error adding member: ${e.message}")
        }
    }

    override suspend fun removeMemberFromSquad(squadId: String, memberId: String) {
        try {
            apiService.removeMemberFromSquad(squadId, mapOf("memberId" to memberId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error removing member: ${e.message}")
        }
    }

    override fun getSquadMessages(squadId: String): Flow<List<SquadMessage>> = flow {
        try {
            emit(apiService.getSquadMessages(squadId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error fetching squad messages: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun sendSquadMessage(squadId: String, text: String) {
        val currentUserId = authRepository.currentUserId ?: return
        try {
            apiService.sendSquadMessage(squadId, mapOf("text" to text, "senderId" to currentUserId))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error sending message: ${e.message}")
        }
    }
}

