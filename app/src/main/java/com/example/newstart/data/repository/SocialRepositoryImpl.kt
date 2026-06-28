package com.example.newstart.data.repository

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

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SocialRepository {

    override fun getFriends(): Flow<List<Friendship>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("friendships")
            .whereArrayContains("userIds", userId)
            .addSnapshotListener { snapshot, _ ->
                val friendships = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("SocialRepository", "Error parsing friendship: ${e.message}")
                        null
                    }
                }
                trySend(friendships ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendFriendRequest(toUserId: String) {
        val fromUserId = auth.currentUser?.uid ?: return
        try {
            val requestData = mapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "status" to FriendshipStatus.PENDING.name,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("friendRequests").add(requestData).await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error sending friend request: ${e.message}", e)
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
            val doc = firestore.collection("friendRequests").document(requestId).get().await()
            val request = doc.toObject(FriendRequest::class.java) ?: return
            
            // 1. Update request status
            firestore.collection("friendRequests").document(requestId)
                .update("status", FriendshipStatus.ACCEPTED.name).await()
                
            // 2. Create friendship
            val friendshipData = mapOf(
                "userIds" to listOf(request.fromUserId, request.toUserId),
                "status" to FriendshipStatus.ACCEPTED.name,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("friendships").add(friendshipData).await()
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error accepting friend request: ${e.message}", e)
        }
    }

    override fun getSquads(): Flow<List<Squad>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("squads")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, _ ->
                val squads = snapshot?.documents?.mapNotNull { it.toObject(Squad::class.java)?.copy(id = it.id) }
                trySend(squads ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createSquad(squad: Squad) {
        val userId = auth.currentUser?.uid ?: return
        val squadWithAdmin = squad.copy(
            adminId = userId,
            members = (squad.members + userId).distinct()
        )
        firestore.collection("squads").add(squadWithAdmin).await()
    }

    override suspend fun joinSquad(squadId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("squads").document(squadId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId)).await()
    }

    override suspend fun leaveSquad(squadId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("squads").document(squadId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(userId)).await()
    }

    override suspend fun updateSquad(squadId: String, name: String, description: String) {
        firestore.collection("squads").document(squadId)
            .update(
                mapOf(
                    "name" to name,
                    "description" to description
                )
            ).await()
    }

    override fun getSocialFeed(): Flow<List<JournalEntry>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var journalsRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        val friendshipsRegistration = firestore.collection("friendships")
            .whereArrayContains("userIds", currentUserId)
            .addSnapshotListener { friendshipsSnapshot, error ->
                if (error != null) {
                    android.util.Log.e("SocialRepository", "Error fetching friendships: ${error.message}", error)
                    return@addSnapshotListener
                }

                val friendIds = friendshipsSnapshot?.documents?.flatMap { doc ->
                    val userIds = doc.get("userIds") as? List<String> ?: emptyList()
                    userIds.filter { it != currentUserId }
                }?.distinct() ?: emptyList()

                val queryUsers = (friendIds + currentUserId).take(30)

                // Hủy listener cũ của journals
                journalsRegistration?.remove()

                if (queryUsers.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                journalsRegistration = firestore.collection("journals")
                    .whereIn("userId", queryUsers)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { journalsSnapshot, journalError ->
                        if (journalError != null) {
                            android.util.Log.e("SocialRepository", "Error fetching journals: ${journalError.message}", journalError)
                            return@addSnapshotListener
                        }

                        if (journalsSnapshot != null) {
                            val entries = journalsSnapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("SocialRepository", "Error parsing journal: ${e.message}")
                                    null
                                }
                            }
                            trySend(entries)
                        }
                    }
            }

        awaitClose {
            friendshipsRegistration.remove()
            journalsRegistration?.remove()
        }
    }

    override suspend fun reactToPost(postId: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val docRef = firestore.collection("journals").document(postId)
            val doc = docRef.get().await()
            val reactions = doc.get("reactions") as? Map<String, String> ?: emptyMap()
            
            if (reactions[userId] == emoji) {
                docRef.update("reactions.$userId", com.google.firebase.firestore.FieldValue.delete()).await()
            } else {
                docRef.update("reactions.$userId", emoji).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "Error reacting to post: ${e.message}", e)
        }
    }

    override suspend fun removeFriend(friendshipId: String) {
        try {
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
        val listener = firestore.collection("squads").document(squadId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.newstart.domain.model.SquadMessage::class.java)?.copy(id = doc.id)
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendSquadMessage(squadId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val senderDoc = firestore.collection("users").document(currentUserId).get().await()
        val senderName = senderDoc.getString("name") ?: "Người dùng"
        
        val message = com.example.newstart.domain.model.SquadMessage(
            senderId = currentUserId,
            senderName = senderName,
            text = text,
            timestamp = java.util.Date()
        )
        firestore.collection("squads").document(squadId).collection("messages").add(message).await()
    }
}
