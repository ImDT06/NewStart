package com.example.newstart.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.newstart.data.remote.dto.*
import com.example.newstart.domain.model.*
import com.example.newstart.domain.repository.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.example.newstart.data.local.dao.SocialDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.newstart.util.AppConstants


@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val socialDao: SocialDao,
    private val apiService: com.example.newstart.data.remote.NewStartApiService,
    @ApplicationContext private val context: Context
) : SocialRepository {

    private val client = OkHttpClient()
    private val friendsRefreshTrigger = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getFriends(): Flow<List<Friendship>> = friendsRefreshTrigger.flatMapLatest {
        callbackFlow {
            val userId = auth.currentUser?.uid ?: ""
            if (userId.isEmpty()) {
                trySend(emptyList())
                return@callbackFlow
            }

            val localJob = launch {
                socialDao.getFriends(userId).collect { entities ->
                    trySend(entities.map { it.toDomain() })
                }
            }

            launch(Dispatchers.IO) {
                try {
                    val response = apiService.getFriends()
                    val friendshipEntities = response.map { dto ->
                        Friendship(id = dto.id, userIds = dto.userIds, status = dto.status).toEntity(cachedForUserId = userId)
                    }
                    socialDao.refreshFriendsTransaction(userId, friendshipEntities)
                } catch (e: Exception) {
                    android.util.Log.e("SocialRepository", "API getFriends error: ${e.message}")
                }
            }
            awaitClose { localJob.cancel() }
        }
    }

    override suspend fun refreshFriends() {
        friendsRefreshTrigger.value += 1
    }

    override fun getIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .addSnapshotListener { snapshot, _ ->
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(FriendRequest::class.java)?.copy(id = doc.id) } catch (e: Exception) { null }
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
                    try { doc.toObject(FriendRequest::class.java)?.copy(id = doc.id) } catch (e: Exception) { null }
                }
                trySend(requests ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendFriendRequest(toUserId: String) {
        try { apiService.sendFriendRequest(toUserId) } catch (e: Exception) {}
    }

    override suspend fun acceptFriendRequest(requestId: String) {
        try { apiService.acceptFriendRequest(requestId) } catch (e: Exception) {}
    }

    private val squadsRefreshTrigger = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getSquads(): Flow<List<Squad>> = squadsRefreshTrigger.flatMapLatest {
        callbackFlow {
            val userId = auth.currentUser?.uid ?: ""
            if (userId.isEmpty()) {
                trySend(emptyList())
                return@callbackFlow
            }

            val localJob = launch {
                socialDao.getSquads(userId).collect { entities ->
                    trySend(entities.map { it.toDomain() })
                }
            }

            launch(Dispatchers.IO) {
                try {
                    val response = apiService.getSquads()
                    val squadEntities = response.map { dto ->
                        Squad(
                            id = dto.id ?: "", name = dto.name, description = dto.description,
                            habitCategory = dto.habitCategory, members = dto.members, adminId = dto.adminId ?: "",
                            createdAt = dto.createdAt.toEpochMilliseconds()?.let { java.util.Date(it) }
                        ).toEntity(cachedForUserId = userId)
                    }
                    socialDao.refreshSquadsTransaction(userId, squadEntities)
                } catch (e: Exception) {
                    android.util.Log.e("SocialRepository", "API getSquads error: ${e.message}")
                }
            }
            awaitClose { localJob.cancel() }
        }
    }

    override suspend fun refreshSquads() {
        squadsRefreshTrigger.value += 1
    }

    override suspend fun createSquad(squad: Squad) {
        try {
            apiService.createSquad(SquadDto(name = squad.name, description = squad.description, habitCategory = squad.habitCategory, members = squad.members))
        } catch (e: Exception) {}
    }

    override suspend fun joinSquad(squadId: String) {
        try { apiService.joinSquad(squadId) } catch (e: Exception) {}
    }

    override suspend fun leaveSquad(squadId: String) {
        try {
            socialDao.deleteSquad(squadId)
            apiService.leaveSquad(squadId)
        } catch (e: Exception) {}
    }

    override suspend fun updateSquad(squadId: String, name: String, description: String) {}

    private val feedRefreshTrigger = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getSocialFeed(): Flow<List<JournalEntry>> = feedRefreshTrigger.flatMapLatest {
        flow {
            try {
                val response = apiService.getSocialFeed()
                val entries = response.map { dto ->
                    JournalEntry(
                        id = dto.id ?: "", userId = dto.userId ?: "", emoji = dto.emoji,
                        text = dto.text, imageUrl = dto.imageUrl,
                        privacy = try { JournalPrivacy.valueOf(dto.privacy) } catch (e: Exception) { JournalPrivacy.FRIENDS },
                        reactions = dto.reactions, timestamp = dto.timestamp.toEpochMilliseconds()?.let { java.util.Date(it) }
                    )
                }
                emit(entries)
            } catch (e: Exception) {
                android.util.Log.e("SocialRepository", "API Feed error: ${e.message}")
                emit(emptyList())
            }
        }
    }

    override suspend fun refreshSocialFeed() {
        feedRefreshTrigger.value += 1
    }

    override suspend fun reactToPost(postId: String, emoji: String) {
        try { apiService.reactToPost(postId, emoji) } catch (e: Exception) {}
    }

    override suspend fun removeFriend(friendshipId: String) {
        try {
            socialDao.deleteFriend(friendshipId)
            firestore.collection("friendships").document(friendshipId).delete().await()
        } catch (e: Exception) {}
    }

    override suspend fun declineFriendRequest(requestId: String) {
        try { firestore.collection("friendRequests").document(requestId).delete().await() } catch (e: Exception) {}
    }

    override suspend fun addMemberToSquad(squadId: String, memberId: String) {
        firestore.collection("squads").document(squadId).update("members", com.google.firebase.firestore.FieldValue.arrayUnion(memberId)).await()
    }

    override suspend fun removeMemberFromSquad(squadId: String, memberId: String) {
        firestore.collection("squads").document(squadId).update("members", com.google.firebase.firestore.FieldValue.arrayRemove(memberId)).await()
    }

    override fun getSquadMessages(squadId: String): Flow<List<com.example.newstart.domain.model.SquadMessage>> = callbackFlow {
        val listener = firestore.collection("squads").document(squadId).collection("messages").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val singleUrl = doc.getString("imageUrl")
                            val urls = doc.get("imageUrls") as? List<*>
                            val imageUrls = urls?.mapNotNull { it as? String } ?: if (singleUrl != null) listOf(singleUrl) else emptyList()
                            val reactionsMap = doc.get("reactions") as? Map<*, *>
                            val reactions = reactionsMap?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap()
                            com.example.newstart.domain.model.SquadMessage(
                                id = doc.id, senderId = doc.getString("senderId") ?: "",
                                senderName = doc.getString("senderName") ?: "Người dùng",
                                text = doc.getString("text") ?: "",
                                imageUrl = singleUrl,
                                imageUrls = imageUrls,
                                timestamp = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date(),
                                reactions = reactions,
                                isRevoked = doc.getBoolean("isRevoked") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendSquadMessage(squadId: String, text: String, imageUrls: List<String>, imageUrl: String?) {
        try { apiService.sendSquadMessage(squadId, SquadMessageDto(text = text, imageUrls = imageUrls, imageUrl = imageUrl)) } catch (e: Exception) {}
    }

    override fun getDirectMessages(friendshipId: String): Flow<List<DirectMessage>> = callbackFlow {
        val listener = firestore.collection("friendships").document(friendshipId).collection("messages").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val singleUrl = doc.getString("imageUrl")
                            val urls = doc.get("imageUrls") as? List<*>
                            val imageUrls = urls?.mapNotNull { it as? String } ?: if (singleUrl != null) listOf(singleUrl) else emptyList()
                            val reactionsMap = doc.get("reactions") as? Map<*, *>
                            val reactions = reactionsMap?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap()
                            DirectMessage(
                                id = doc.id, senderId = doc.getString("senderId") ?: "",
                                senderName = doc.getString("senderName") ?: "Người dùng",
                                text = doc.getString("text") ?: "",
                                imageUrl = singleUrl,
                                imageUrls = imageUrls,
                                timestamp = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date(),
                                sharedJournalId = doc.getString("sharedJournalId"),
                                sharedJournalText = doc.getString("sharedJournalText"),
                                sharedJournalImageUrl = doc.getString("sharedJournalImageUrl"),
                                sharedJournalEmoji = doc.getString("sharedJournalEmoji"),
                                sharedJournalAuthorName = doc.getString("sharedJournalAuthorName"),
                                reactions = reactions,
                                isRevoked = doc.getBoolean("isRevoked") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendDirectMessage(
        friendshipId: String,
        text: String,
        sharedJournal: JournalEntry?,
        imageUrls: List<String>,
        imageUrl: String?
    ): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            
            // Đảm bảo parent document (friendship) tồn tại trong Firestore để tránh lỗi bảo mật subcollection
            val friendshipRef = firestore.collection("friendships").document(friendshipId)
            val friendshipDoc = friendshipRef.get().await()
            if (!friendshipDoc.exists()) {
                val friendshipEntity = socialDao.getFriendshipById(friendshipId)
                val userIdsList = friendshipEntity?.userIds?.split(",")?.filter { it.isNotEmpty() }
                    ?: listOf(user.uid) // fallback
                
                val friendshipData = mapOf(
                    "userIds" to userIdsList,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "status" to "ACCEPTED"
                )
                friendshipRef.set(friendshipData).await()
            }

            val messageData = mutableMapOf<String, Any>(
                "senderId" to user.uid,
                "senderName" to (user.displayName ?: "Người dùng"),
                "text" to text,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            if (imageUrls.isNotEmpty()) {
                messageData["imageUrls"] = imageUrls
            }
            imageUrl?.let { messageData["imageUrl"] = it }
            
            if (sharedJournal != null) {
                messageData["sharedJournalId"] = sharedJournal.id
                messageData["sharedJournalText"] = sharedJournal.text
                sharedJournal.imageUrl?.let { messageData["sharedJournalImageUrl"] = it }
                messageData["sharedJournalEmoji"] = sharedJournal.emoji
                
                var authorName = "Người dùng"
                try {
                    val authorDoc = firestore.collection("users").document(sharedJournal.userId).get().await()
                    authorName = authorDoc.getString("name") ?: "Người dùng"
                } catch (e: Exception) {}
                messageData["sharedJournalAuthorName"] = authorName
            }
            
            firestore.collection("friendships").document(friendshipId).collection("messages").document().set(messageData).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun getLastMessage(friendshipId: String): Flow<DirectMessage?> = callbackFlow {
        val listener = firestore.collection("friendships").document(friendshipId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(1)
            .addSnapshotListener { snapshot, _ ->
                val doc = snapshot?.documents?.firstOrNull()
                if (doc != null) {
                    try {
                        val reactionsMap = doc.get("reactions") as? Map<*, *>
                        val reactions = reactionsMap?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap()
                        trySend(DirectMessage(
                            id = doc.id, senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "Người dùng",
                            text = doc.getString("text") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            timestamp = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date(),
                            sharedJournalId = doc.getString("sharedJournalId"),
                            sharedJournalText = doc.getString("sharedJournalText"),
                            sharedJournalImageUrl = doc.getString("sharedJournalImageUrl"),
                            sharedJournalEmoji = doc.getString("sharedJournalEmoji"),
                            sharedJournalAuthorName = doc.getString("sharedJournalAuthorName"),
                            reactions = reactions
                        ))
                    } catch (e: Exception) { trySend(null) }
                } else { trySend(null) }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadImage(uri: android.net.Uri): Result<String> {
        return try {
            val url = uploadToCloudinary(uri)
            if (url != null) {
                Result.success(url)
            } else {
                Result.failure(Exception("Upload to Cloudinary failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadToCloudinary(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val bytes = compressImage(uri) ?: return@withContext null
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "image.jpg", 
                    bytes.toRequestBody("image/*".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", AppConstants.CLOUDINARY_UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url(AppConstants.CLOUDINARY_UPLOAD_URL)
                .post(requestBody)
                .build()

            val call = client.newCall(request)
            
            val response = suspendCancellableCoroutine<Response> { continuation ->
                continuation.invokeOnCancellation {
                    call.cancel()
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                })
            }

            response.use { resp ->
                val responseData = resp.body?.string()
                
                if (resp.isSuccessful && responseData != null) {
                    val jsonObject = JSONObject(responseData)
                    jsonObject.getString("secure_url")
                } else {
                    android.util.Log.e("Cloudinary", "Upload failed: $responseData")
                    null
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("Cloudinary", "Upload failed: ${e.message}", e)
            null
        }
    }

    private fun compressImage(uri: android.net.Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            val maxDimension = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val (newWidth, newHeight) = if (width > height) {
                if (width > maxDimension) {
                    val ratio = width.toFloat() / maxDimension
                    maxDimension to (height / ratio).toInt()
                } else width to height
            } else {
                if (height > maxDimension) {
                    val ratio = height.toFloat() / maxDimension
                    (width / ratio).toInt() to maxDimension
                } else width to height
            }

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Compression failed: ${e.message}")
            null
        }
    }

    override suspend fun reactToSquadMessage(squadId: String, messageId: String, emoji: String) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val docRef = firestore.collection("squads").document(squadId)
                .collection("messages").document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val reactions = snapshot.get("reactions") as? Map<*, *>
                val currentEmoji = reactions?.get(userId) as? String
                
                if (currentEmoji == emoji) {
                    transaction.update(docRef, "reactions.$userId", com.google.firebase.firestore.FieldValue.delete())
                } else {
                    transaction.update(docRef, "reactions.$userId", emoji)
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error reacting to squad message: ${e.message}")
        }
    }

    override suspend fun reactToDirectMessage(friendshipId: String, messageId: String, emoji: String) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val docRef = firestore.collection("friendships").document(friendshipId)
                .collection("messages").document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val reactions = snapshot.get("reactions") as? Map<*, *>
                val currentEmoji = reactions?.get(userId) as? String
                
                if (currentEmoji == emoji) {
                    transaction.update(docRef, "reactions.$userId", com.google.firebase.firestore.FieldValue.delete())
                } else {
                    transaction.update(docRef, "reactions.$userId", emoji)
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error reacting to direct message: ${e.message}")
        }
    }

    override suspend fun revokeSquadMessage(squadId: String, messageId: String) {
        try {
            val docRef = firestore.collection("squads").document(squadId)
                .collection("messages").document(messageId)
            docRef.update(
                mapOf(
                    "isRevoked" to true,
                    "text" to "",
                    "imageUrls" to emptyList<String>(),
                    "imageUrl" to null,
                    "reactions" to emptyMap<String, String>()
                )
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error revoking squad message: ${e.message}")
        }
    }

    override suspend fun revokeDirectMessage(friendshipId: String, messageId: String) {
        try {
            val docRef = firestore.collection("friendships").document(friendshipId)
                .collection("messages").document(messageId)
            docRef.update(
                mapOf(
                    "isRevoked" to true,
                    "text" to "",
                    "imageUrls" to emptyList<String>(),
                    "imageUrl" to null,
                    "reactions" to emptyMap<String, String>()
                )
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error revoking direct message: ${e.message}")
        }
    }
}
