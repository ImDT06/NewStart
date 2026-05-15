package com.example.newstart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.repository.JournalRepository
import com.example.newstart.util.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
) : JournalRepository {

    private val client = OkHttpClient()

    override suspend fun saveJournalEntry(emoji: String, text: String, imageUri: Uri?): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            var imageUrl: String? = null

            // Upload ảnh lên Cloudinary nếu có
            if (imageUri != null) {
                imageUrl = withContext(Dispatchers.IO) {
                    uploadToCloudinary(imageUri)
                }
                if (imageUrl == null) throw Exception("Failed to upload image to Cloudinary")
            }

            val entry = JournalEntry(
                userId = userId,
                emoji = emoji,
                text = text,
                imageUrl = imageUrl,
                timestamp = Date()
            )

            // Sử dụng document() để lấy reference trước, lấy ID sau đó mới set data
            val docRef = firestore.collection("journals").document()
            val entryWithId = entry.copy(id = docRef.id)
            
            docRef.set(entryWithId).await()
            android.util.Log.d("JournalRepository", "Entry saved successfully with ID: ${docRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Error saving entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        return try {
            firestore.collection("journals").document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Error deleting entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun uploadToCloudinary(uri: Uri): String? {
        return try {
            val bytes = compressImage(uri) ?: return null
            
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

            client.newCall(request).execute().use { response ->
                val responseData = response.body?.string()
                
                if (response.isSuccessful && responseData != null) {
                    val jsonObject = JSONObject(responseData)
                    return jsonObject.getString("secure_url")
                } else {
                    val errorMsg = try {
                        JSONObject(responseData ?: "{}").getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        "Mã lỗi ${response.code}"
                    }
                    
                    // Hiện lỗi trực tiếp lên màn hình để bạn dễ debug
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Cloudinary: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                    
                    android.util.Log.e("Cloudinary", "Upload failed: $responseData")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Cloudinary", "Upload failed: ${e.message}", e)
            null
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            // Tính toán tỉ lệ để resize (Max 1024px)
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
            // Nén chất lượng 80%
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Compression failed: ${e.message}")
            null
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
                    android.util.Log.e("JournalRepository", "Error fetching entries: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    android.util.Log.d("JournalRepository", "Raw documents count: ${snapshot.size()}")
                    val entries = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("JournalRepository", "Error parsing doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    android.util.Log.d("JournalRepository", "Successfully parsed entries: ${entries.size}")
                    trySend(entries)
                }
            }
        awaitClose { subscription.remove() }
    }
}
