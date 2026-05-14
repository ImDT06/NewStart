package com.example.newstart.data.repository

import android.net.Uri
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.repository.JournalRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : JournalRepository {

    override suspend fun saveJournalEntry(emoji: String, text: String, imageUri: Uri?): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            var imageUrl: String? = null

            // Upload image to Firebase Storage if exists
            if (imageUri != null) {
                val fileName = "journal_${userId}_${System.currentTimeMillis()}.jpg"
                val storageRef = storage.reference.child("journals/$userId/$fileName")
                storageRef.putFile(imageUri).await()
                imageUrl = storageRef.downloadUrl.await().toString()
            }

            // Create Journal Entry
            val entry = JournalEntry(
                userId = userId,
                emoji = emoji,
                text = text,
                imageUrl = imageUrl,
                timestamp = Date()
            )

            // Save to Firestore
            firestore.collection("journals").add(entry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getJournalEntries(): Flow<List<JournalEntry>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            return@callbackFlow
        }

        val subscription = firestore.collection("journals")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entries = snapshot.toObjects(JournalEntry::class.java)
                    trySend(entries)
                }
            }

        awaitClose { subscription.remove() }
    }
}
