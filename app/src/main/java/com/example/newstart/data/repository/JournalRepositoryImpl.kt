package com.example.newstart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.newstart.data.remote.ApiService
import com.example.newstart.domain.model.BookDetails
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.model.MovieDetails
import com.example.newstart.domain.model.SubjectDetails
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.JournalRepository
import com.example.newstart.util.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
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
        subjectDetails: SubjectDetails?
    ): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId ?: throw Exception("User not logged in")
            var imageUrl: String? = null

            // Upload ảnh lên Cloudinary nếu có
            if (imageUri != null) {
                imageUrl = uploadToCloudinary(imageUri)
                if (imageUrl == null) throw Exception("Failed to upload image to Cloudinary")
            }

            val entry = JournalEntry(
                userId = userId,
                emoji = emoji,
                text = text,
                imageUrl = imageUrl,
                imageSource = imageSource,
                type = type,
                movieDetails = movieDetails,
                bookDetails = bookDetails,
                subjectDetails = subjectDetails,
                timestamp = Date()
            )

            // Gui api de luu vao backend
            apiService.saveJournal(entry)
            android.util.Log.d("JournalRepository", "Entry saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("JournalRepository", "Error saving entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        return try {
            apiService.deleteJournal(entryId)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Error deleting entry: ${e.message}", e)
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
            android.util.Log.e("JournalRepository", "Compression failed: ${e.message}")
            null
        }
    }

    override fun getJournalEntries(): Flow<List<JournalEntry>> = flow {
        val userId = authRepository.currentUserId
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            val entries = apiService.getJournals(userId)
            emit(entries)
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Error fetching entries: ${e.message}", e)
            emit(emptyList())
        }
    }
}

