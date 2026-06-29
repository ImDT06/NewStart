package com.example.newstart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.newstart.data.remote.dto.*
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.model.MovieDetails
import com.example.newstart.domain.model.BookDetails
import com.example.newstart.domain.model.SubjectDetails
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.IOException
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
    private val apiService: com.example.newstart.data.remote.NewStartApiService,
    @ApplicationContext private val context: Context,
) : JournalRepository {

    private val client = OkHttpClient()

    override suspend fun saveJournalEntry(
        emoji: String,
        text: String,
        imageUri: Uri?,
        imageSource: String?,
        type: JournalType,
        movieDetails: MovieDetails?,
        bookDetails: BookDetails?,
        subjectDetails: SubjectDetails?,
        privacy: JournalPrivacy
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            var imageUrl: String? = null

            // 1. Hồn: Upload ảnh lên Cloudinary nếu có
            if (imageUri != null) {
                imageUrl = uploadToCloudinary(imageUri)
                if (imageUrl == null) throw Exception("Failed to upload image to Cloudinary")
            }

            // 2. Xác: Chuẩn bị dữ liệu gửi lên Server dưới dạng DTO
            val entryDto = JournalEntryDto(
                emoji = emoji,
                text = text,
                imageUrl = imageUrl,
                imageSource = imageSource ?: "",
                type = type.name,
                privacy = privacy.name,
                movieDetails = movieDetails?.let {
                    MovieDetailsDto(
                        title = it.title,
                        director = it.director,
                        actors = it.actors,
                        rating = it.rating
                    )
                },
                bookDetails = bookDetails?.let {
                    BookDetailsDto(
                        title = it.title,
                        author = it.author,
                        pagesRead = it.pagesRead,
                        rating = it.rating
                    )
                },
                subjectDetails = subjectDetails?.let {
                    SubjectDetailsDto(
                        name = it.name,
                        topic = it.topic,
                        score = it.score,
                        understandingLevel = it.understandingLevel
                    )
                }
            )

            // 3. Gửi lên Spring Boot API
            println(">>> Đang gửi Nhật ký lên Server...")
            apiService.createJournalEntry(entryDto)
            
            android.util.Log.d("JournalRepository", "Entry saved via API successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("JournalRepository", "Error saving entry via API: ${e.message}", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Lỗi lưu nhật ký: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    override suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        return try {
            apiService.deleteJournalEntry(entryId)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Error deleting entry via API: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadToCloudinary(uri: Uri): String? = withContext(Dispatchers.IO) {
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
                    val errorMsg = try {
                        JSONObject(responseData ?: "{}").getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        "Mã lỗi ${resp.code}"
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
            if (e is kotlinx.coroutines.CancellationException) throw e
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
