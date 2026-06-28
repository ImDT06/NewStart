package com.example.newstart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.newstart.data.remote.ApiService
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.UserRepository
import com.example.newstart.util.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : UserRepository {
    
    private val client = OkHttpClient()

    override fun getUserById(id: String): Flow<User> = flow {
        if (id.isBlank()) {
            emit(User())
            return@flow
        }
        try {
            val user = apiService.getUserById(id)
            emit(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error getting user: ${e.message}")
            // Tra ve user voi id thoi nhu mot fallback
            emit(User(id = id))
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

            // 2. Call API to update avatar
            val updates = mutableMapOf<String, String>()
            imageUrl?.let { updates["avatarUrl"] = it }
            
            apiService.updateAvatar(userId, updates)
            
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
            val updates = mapOf("name" to name)
            apiService.updateProfile(userId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): List<User> {
        val queryClean = query.trim()
        if (queryClean.isEmpty()) return emptyList()

        return try {
            apiService.searchUsers(queryClean)
        } catch (e: Exception) {
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

