package com.example.newstart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.UserRepository
import com.example.newstart.util.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
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
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : UserRepository {
    
    private val client = OkHttpClient()

    override fun getUserById(id: String): Flow<User> = callbackFlow {
        if (id.isBlank()) {
            trySend(User())
            close()
            return@callbackFlow
        }

        val subscription = firestore.collection("users").document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("UserRepository", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val user = snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
                        if (user != null) trySend(user)
                    } catch (e: Exception) {
                        android.util.Log.e("UserRepository", "Error parsing user: ${e.message}")
                    }
                } else {
                    // Fallback: Lấy từ Auth nếu Firestore chưa có (user mới)
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null && firebaseUser.uid == id) {
                        trySend(User(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString()
                        ))
                    }
                }
            }
        awaitClose { subscription.remove() }
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

            // 2. LẤY DỮ LIỆU TỪ AUTH ĐỂ LẮP VÀO FIRESTORE
            val firebaseUser = auth.currentUser
            val userDocument = firestore.collection("users").document(userId)
            
            // Đóng gói thông tin từ Auth để gửi qua Firestore
            val updates = mutableMapOf<String, Any?>()
            updates["id"] = userId
            updates["avatarUrl"] = imageUrl
            
            // Lấy Tên và Email từ Auth, nếu có thì mới lắp vào để tránh đè dữ liệu trống
            firebaseUser?.displayName?.let { if (it.isNotBlank()) updates["name"] = it }
            firebaseUser?.email?.let { if (it.isNotBlank()) updates["email"] = it }
            
            // Thực hiện lệnh lắp dữ liệu (Merge giúp giữ lại các trường khác nếu có)
            userDocument.set(updates, SetOptions.merge()).await()

            // 3. Cập nhật ngược lại hồ sơ Auth để đồng bộ bộ nhớ tạm
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
                    
                    // Hiện Toast để bạn biết tại sao lỗi (VD: sai Preset Cloudinary)
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
