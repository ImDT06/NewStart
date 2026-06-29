package com.example.newstart.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.newstart.data.remote.dto.*
import com.example.newstart.domain.model.*
import com.example.newstart.domain.repository.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.example.newstart.data.local.dao.SocialDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch


@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val socialDao: SocialDao,
    private val apiService: com.example.newstart.data.remote.NewStartApiService,
    @ApplicationContext private val context: Context
) : SocialRepository {

    override fun getFriends(): Flow<List<Friendship>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }

        // 1. Phát ra dữ liệu từ Room cache cục bộ trước
        val localJob = launch {
            socialDao.getFriends(userId).collect { entities ->
                trySend(entities.map { it.toDomain() })
            }
        }

        // 2. Chạy ngầm gọi API để cập nhật cache
        launch(Dispatchers.IO) {
            try {
                val response = apiService.getFriends()
                val friendshipEntities = response.map { dto ->
                    Friendship(
                        id = dto.id,
                        userIds = dto.userIds,
                        status = dto.status
                    ).toEntity(cachedForUserId = userId)
                }
                
                // Xóa cache cũ của user và ghi đè mới
                socialDao.clearFriends(userId)
                if (friendshipEntities.isNotEmpty()) {
                    socialDao.insertFriends(friendshipEntities)
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialRepository", "API getFriends error: ${e.message}", e)
            }
        }

        awaitClose {
            localJob.cancel()
        }
    }

    override suspend fun sendFriendRequest(toUserId: String) {
        try {
            apiService.sendFriendRequest(toUserId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API Request error: ${e.message}")
        }
    }

    override fun getIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .addSnapshotListener { snapshot, _ ->
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("SocialRepository", "Error parsing incoming request: ${e.message}")
                        null
                    }
                }
                trySend(requests ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getSentRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .addSnapshotListener { snapshot, _ ->
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("SocialRepository", "Error parsing sent request: ${e.message}")
                        null
                    }
                }
                trySend(requests ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun acceptFriendRequest(requestId: String) {
        try {
            apiService.acceptFriendRequest(requestId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API Accept error: ${e.message}")
        }
    }

    override fun getSquads(): Flow<List<Squad>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }

        // 1. Phát ra dữ liệu từ Room cache cục bộ trước
        val localJob = launch {
            socialDao.getSquads(userId).collect { entities ->
                trySend(entities.map { it.toDomain() })
            }
        }

        // 2. Chạy ngầm gọi API để cập nhật cache
        launch(Dispatchers.IO) {
            try {
                val response = apiService.getSquads()
                val squadEntities = response.map { dto ->
                    Squad(
                        id = dto.id ?: "",
                        name = dto.name,
                        description = dto.description,
                        habitCategory = dto.habitCategory,
                        members = dto.members,
                        adminId = dto.adminId ?: "",
                        createdAt = dto.createdAt.toEpochMilliseconds()?.let { java.util.Date(it) }
                    ).toEntity(cachedForUserId = userId)
                }

                // Xóa cache cũ của user và ghi đè mới
                socialDao.clearSquads(userId)
                if (squadEntities.isNotEmpty()) {
                    socialDao.insertSquads(squadEntities)
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialRepository", "API getSquads error: ${e.message}", e)
            }
        }

        awaitClose {
            localJob.cancel()
        }
    }

    override suspend fun createSquad(squad: Squad) {
        try {
            val squadDto = SquadDto(
                name = squad.name,
                description = squad.description,
                habitCategory = squad.habitCategory,
                members = squad.members
            )
            apiService.createSquad(squadDto)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API Create Squad error: ${e.message}")
        }
    }

    override suspend fun joinSquad(squadId: String) {
        try {
            apiService.joinSquad(squadId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API Join Squad error: ${e.message}")
        }
    }

    override suspend fun leaveSquad(squadId: String) {
        try {
            // Xóa nhanh dưới local cache trước
            socialDao.deleteSquad(squadId)
            
            apiService.leaveSquad(squadId)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API Leave Squad error: ${e.message}")
        }
    }

    override suspend fun updateSquad(squadId: String, name: String, description: String) {
        // Tạm thời để trống hoặc gọi API update nếu có
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getSocialFeed(): Flow<List<JournalEntry>> = refreshTrigger.flatMapLatest {
        flow {
            try {
                val response = apiService.getSocialFeed()
                val entries = response.map { dto ->
                    JournalEntry(
                        id = dto.id ?: "",
                        userId = dto.userId ?: "",
                        emoji = dto.emoji,
                        text = dto.text,
                        imageUrl = dto.imageUrl,
                        privacy = try { com.example.newstart.domain.model.JournalPrivacy.valueOf(dto.privacy) } catch (e: Exception) { com.example.newstart.domain.model.JournalPrivacy.FRIENDS },
                        reactions = dto.reactions,
                        timestamp = dto.timestamp.toEpochMilliseconds()?.let { java.util.Date(it) }
                    )
                }
                emit(entries)
            } catch (e: Exception) {
                android.util.Log.e("SocialRepository", "API Feed error: ${e.message}", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Lỗi tải bảng tin: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                emit(emptyList())
            }
        }
    }

    override suspend fun refreshSocialFeed() {
        refreshTrigger.emit(Unit)
    }

    override suspend fun reactToPost(postId: String, emoji: String) {
        try {
            apiService.reactToPost(postId, emoji)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API React error: ${e.message}")
        }
    }

    override suspend fun removeFriend(friendshipId: String) {
        try {
            // Xóa nhanh dưới local cache trước
            socialDao.deleteFriend(friendshipId)
            
            val doc = firestore.collection("friendships").document(friendshipId).get().await()
            val userIds = doc.get("userIds") as? List<String> ?: emptyList()
            
            firestore.collection("friendships").document(friendshipId).delete().await()
            
            if (userIds.size == 2) {
                val u1 = userIds[0]
                val u2 = userIds[1]
                
                val reqs1 = firestore.collection("friendRequests")
                    .whereEqualTo("fromUserId", u1)
                    .whereEqualTo("toUserId", u2)
                    .get().await()
                    
                val reqs2 = firestore.collection("friendRequests")
                    .whereEqualTo("fromUserId", u2)
                    .whereEqualTo("toUserId", u1)
                    .get().await()
                    
                for (d in reqs1.documents) {
                    d.reference.delete()
                }
                for (d in reqs2.documents) {
                    d.reference.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error removing friend: ${e.message}", e)
        }
    }

    override suspend fun declineFriendRequest(requestId: String) {
        try {
            firestore.collection("friendRequests").document(requestId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error declining friend request: ${e.message}", e)
        }
    }

    override suspend fun addMemberToSquad(squadId: String, memberId: String) {
        firestore.collection("squads").document(squadId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(memberId)).await()
    }

    override suspend fun removeMemberFromSquad(squadId: String, memberId: String) {
        firestore.collection("squads").document(squadId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(memberId)).await()
    }

    override fun getSquadMessages(squadId: String): Flow<List<com.example.newstart.domain.model.SquadMessage>> = callbackFlow {
        val listener = firestore.collection("squads")
            .document(squadId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("SocialRepository", "Error listening to squad messages: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: "Người dùng"
                            val text = doc.getString("text") ?: ""
                            val timestampDate = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date()
                            
                            com.example.newstart.domain.model.SquadMessage(
                                id = id,
                                senderId = senderId,
                                senderName = senderName,
                                text = text,
                                timestamp = timestampDate
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("SocialRepository", "Error parsing message: ${e.message}")
                            null
                        }
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendSquadMessage(squadId: String, text: String) {
        try {
            apiService.sendSquadMessage(squadId, SquadMessageDto(text = text))
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "API Send Message error: ${e.message}")
        }
    }
}
