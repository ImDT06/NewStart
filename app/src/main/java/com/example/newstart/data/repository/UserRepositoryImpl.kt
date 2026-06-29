package com.example.newstart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.newstart.data.remote.dto.*
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.UserRepository
import com.example.newstart.util.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
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
import javax.inject.Inject
import javax.inject.Singleton
import com.example.newstart.data.local.dao.UserDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val apiService: com.example.newstart.data.remote.NewStartApiService,
    @ApplicationContext private val context: Context
) : UserRepository {
    
    private val client = OkHttpClient()

    override fun getUserById(id: String): Flow<User> = callbackFlow {
        if (id.isBlank()) {
            trySend(User())
            close()
            return@callbackFlow
        }

        // 1. Phát ra dữ liệu từ Room cache cục bộ trước (chỉ phát khi không phải null để giữ trạng thái đang tải ban đầu)
        val localJob = launch {
            userDao.getUserById(id).collect { entity ->
                if (entity != null) {
                    trySend(entity.toDomain())
                }
            }
        }

        // 2. Chạy ngầm gọi API để cập nhật cache
        launch(Dispatchers.IO) {
            try {
                val dto = apiService.getUserById(id)
                val user = User(
                    id = dto.id ?: "",
                    userId = dto.userId ?: "",
                    name = dto.name,
                    email = dto.email,
                    avatarUrl = dto.avatarUrl
                )
                userDao.insertUser(user.toEntity())
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "API getUserById error: ${e.message}")
                val firebaseUser = auth.currentUser
                if (firebaseUser != null && firebaseUser.uid == id) {
                    val selfUser = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        avatarUrl = firebaseUser.photoUrl?.toString()
                    )
                    userDao.insertUser(selfUser.toEntity())
                }
            }
        }

        awaitClose {
            localJob.cancel()
        }
    }

    override suspend fun updateAvatar(userId: String, uri: Uri?): Result<String?> {
        if (userId.isBlank()) return Result.failure(Exception("User ID is empty"))
        
        return try {
            var imageUrl: String? = null

            // 1. Upload Cloudinary
            if (uri != null) {
                imageUrl = withContext(Dispatchers.IO) {
                    uploadToCloudinary(uri)
                }
                if (imageUrl == null) throw Exception("Cloudinary upload failed")
            }

            // 2. Gửi API update
            val updates = mutableMapOf<String, String>()
            imageUrl?.let { updates["avatarUrl"] = it }
            
            apiService.updateProfile(updates)

            // 3. Cập nhật hồ sơ Auth
            val firebaseUser = auth.currentUser
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(imageUrl?.let { Uri.parse(it) })
                .build()
            firebaseUser?.updateProfile(profileUpdates)?.await()
            firebaseUser?.reload()?.await()
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show()
            }
            
            Result.success(imageUrl)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Update error: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(userId: String, name: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(Exception("User ID is empty"))
        
        return try {
            val firebaseUser = auth.currentUser
            
            // Gửi API update
            val updates = mapOf("name" to name)
            apiService.updateProfile(updates)

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser?.updateProfile(profileUpdates)?.await()
            firebaseUser?.reload()?.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): List<User> {
        val queryClean = query.trim()
        if (queryClean.isEmpty()) return emptyList()

        return try {
            val response = apiService.searchUsers(queryClean)
            response.map { dto ->
                User(
                    id = dto.id ?: "",
                    userId = dto.userId ?: "",
                    name = dto.name,
                    email = dto.email,
                    avatarUrl = dto.avatarUrl
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "API Search error: ${e.message}")
            emptyList()
        }
    }

    private fun uploadToCloudinary(uri: Uri): String? {
        return try {
            val bytes = compressImage(uri) ?: return null
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "avatar_${System.currentTimeMillis()}.jpg", 
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
                    
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Cloudinary: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            apiService.updateProfile(mapOf("fcmToken" to token))
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "API updateFcmToken error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateProfileFields(updates: Map<String, String>): Result<Unit> {
        return try {
            apiService.updateProfile(updates)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "API updateProfileFields error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
