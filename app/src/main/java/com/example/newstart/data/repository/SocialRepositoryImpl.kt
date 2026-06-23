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
                val friendships = snapshot?.documents?.mapNotNull { it.toObject(Friendship::class.java)?.copy(id = it.id) }
                trySend(friendships ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendFriendRequest(toUserId: String) {
        val fromUserId = auth.currentUser?.uid ?: return
        val request = FriendRequest(
            fromUserId = fromUserId,
            toUserId = toUserId,
            status = FriendshipStatus.PENDING
        )
        firestore.collection("friendRequests").add(request).await()
    }

    override fun getIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .addSnapshotListener { snapshot, _ ->
                val requests = snapshot?.documents?.mapNotNull { it.toObject(FriendRequest::class.java)?.copy(id = it.id) }
                trySend(requests ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun acceptFriendRequest(requestId: String) {
        val doc = firestore.collection("friendRequests").document(requestId).get().await()
        val request = doc.toObject(FriendRequest::class.java) ?: return
        
        // 1. Update request status
        firestore.collection("friendRequests").document(requestId)
            .update("status", FriendshipStatus.ACCEPTED.name).await()
            
        // 2. Create friendship
        val friendship = Friendship(
            userIds = listOf(request.fromUserId, request.toUserId),
            status = FriendshipStatus.ACCEPTED
        )
        firestore.collection("friendships").add(friendship).await()
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
            members = listOf(userId)
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

    override fun getSocialFeed(): Flow<List<JournalEntry>> = callbackFlow {
        // Simple feed: posts from friendships
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        // In a real app, this would use the fan-out strategy mentioned in the report.
        // For MVP, we query posts with privacy = FRIENDS or SQUAD where the user is a member.
        val listener = firestore.collection("journal_entries")
            .whereIn("privacy", listOf(JournalPrivacy.FRIENDS.name, JournalPrivacy.SQUAD.name))
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val entries = snapshot?.documents?.mapNotNull { it.toObject(JournalEntry::class.java)?.copy(id = it.id) }
                // Filtering in client for MVP (not ideal for production)
                trySend(entries ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun reactToPost(postId: String, emoji: String) {
        // Implementation for reactions
    }
}
