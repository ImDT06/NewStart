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
import com.example.newstart.domain.model.JournalPrivacy
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
import com.example.newstart.data.local.dao.JournalDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import androidx.work.*
import com.example.newstart.data.worker.JournalSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val journalDao: JournalDao,
    private val apiService: com.example.newstart.data.remote.NewStartApiService,
    @ApplicationContext private val context: Context,
) : JournalRepository {

    private val client = OkHttpClient()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<JournalSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniqueWork(
            "journal_sync_work",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

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
            

            val entryId = UUID.randomUUID().toString()
            
            // 1. Lưu local trước (isSynced = false)
            val localEntry = JournalEntry(
                id = entryId,
                userId = userId,
                emoji = emoji,
                text = text,
                imageUrl = null,
                imageSource = imageSource,
                linkedHabitId = null,
                linkedTodoId = null,
                privacy = privacy,
                type = type,
                movieDetails = movieDetails,
                bookDetails = bookDetails,
                subjectDetails = subjectDetails,
                timestamp = Date()
            )
            journalDao.insertJournal(localEntry.toEntity(isSynced = false, localImageUri = imageUri?.toString()))
            
            // 2. Thử đồng bộ ngay lập tức
            try {
                var imageUrl: String? = null
                if (imageUri != null) {
                    imageUrl = uploadToCloudinary(imageUri)
                    if (imageUrl == null) throw Exception("Failed to upload image to Cloudinary")
                }

                val entryDto = JournalEntryDto(
                    id = entryId,
                    userId = userId,
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

                println(">>> Đang gửi Nhật ký lên Server...")
                apiService.createJournalEntry(entryDto)
                
                // Đồng bộ thành công -> update trạng thái isSynced = true
                journalDao.insertJournal(
                    localEntry.copy(imageUrl = imageUrl).toEntity(isSynced = true, localImageUri = imageUri?.toString())
                )
                android.util.Log.d("JournalRepository", "Entry saved via API successfully")
            } catch (e: Exception) {
                // Lỗi mạng -> Lên lịch sync ngầm
                scheduleSync()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("JournalRepository", "Error saving entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        return try {
            // Xóa local trước để UI mượt mà
            journalDao.deleteJournal(entryId)
            
            try {
                apiService.deleteJournalEntry(entryId)
            } catch (e: Exception) {
                // Bỏ qua lỗi xóa trên remote
            }
            Result.success(Unit)
        } catch (e: Exception) {
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

        // 1. Phát ra dữ liệu từ Room cục bộ
        val localJob = launch {
            journalDao.getJournalEntries(userId).collect { entities ->
                trySend(entities.map { it.toDomain() })
            }
        }

        // 2. Lắng nghe Firestore để đồng bộ các thay đổi từ xa vào Room
        val subscription = firestore.collection("journals")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("JournalRepository", "Error fetching entries: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remoteEntries = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    launch(Dispatchers.IO) {
                        // A. Dọn dẹp các journal đã bị xóa trên Firestore khỏi database cục bộ
                        val localSyncedIds = journalDao.getSyncedJournalIds(userId)
                        val remoteIds = remoteEntries.map { it.id }.toSet()
                        val deletedIds = localSyncedIds.filter { !remoteIds.contains(it) }
                        
                        for (deletedId in deletedIds) {
                            journalDao.deleteJournal(deletedId)
                        }

                        // B. Thêm/Cập nhật các journal mới từ Firestore vào Room
                        if (remoteEntries.isNotEmpty()) {
                            val entities = remoteEntries.map { it.toEntity(isSynced = true) }
                            journalDao.insertJournals(entities)
                        }
                    }
                }
            }
        awaitClose {
            localJob.cancel()
            subscription.remove()
        }
    }

    override suspend fun syncUnsyncedJournals(): Result<Unit> {
        return try {
            val unsynced = journalDao.getUnsyncedJournals()
            android.util.Log.d("JournalRepository", "Syncing ${unsynced.size} unsynced journals")
            
            for (entity in unsynced) {
                var currentImageUrl = entity.imageUrl
                
                // 1. Tải ảnh lên Cloudinary nếu chưa tải lên và có ảnh cục bộ
                if (currentImageUrl == null && !entity.localImageUri.isNullOrEmpty()) {
                    val localUri = Uri.parse(entity.localImageUri)
                    currentImageUrl = uploadToCloudinary(localUri)
                    if (currentImageUrl == null) {
                        throw Exception("Failed to upload image during sync")
                    }
                }
                
                // 2. Gửi API lên Spring Boot
                val entryDto = JournalEntryDto(
                    id = entity.id,
                    userId = entity.userId,
                    emoji = entity.emoji,
                    text = entity.text,
                    imageUrl = currentImageUrl,
                    imageSource = entity.imageSource ?: "",
                    type = entity.type,
                    privacy = entity.privacy,
                    movieDetails = if (entity.movieTitle != null) MovieDetailsDto(
                        title = entity.movieTitle,
                        director = entity.movieDirector ?: "",
                        actors = entity.movieActors ?: "",
                        rating = entity.movieRating ?: 0f
                    ) else null,
                    bookDetails = if (entity.bookTitle != null) BookDetailsDto(
                        title = entity.bookTitle,
                        author = entity.bookAuthor ?: "",
                        pagesRead = entity.bookPagesRead ?: 0,
                        rating = entity.bookRating ?: 0f
                    ) else null,
                    subjectDetails = if (entity.subjectName != null) SubjectDetailsDto(
                        name = entity.subjectName,
                        topic = entity.subjectTopic ?: "",
                        score = entity.subjectScore,
                        understandingLevel = entity.subjectUnderstandingLevel ?: 3
                    ) else null
                )
                
                apiService.createJournalEntry(entryDto)
                
                // 3. Đánh dấu đã synced trong Room
                journalDao.insertJournal(
                    entity.copy(
                        imageUrl = currentImageUrl,
                        isSynced = true
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("JournalRepository", "Error in syncUnsyncedJournals: ${e.message}", e)
            Result.failure(e)
        }
    }
}
