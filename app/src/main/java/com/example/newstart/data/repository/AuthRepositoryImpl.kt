package com.example.newstart.data.repository

import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val apiService: com.example.newstart.data.remote.NewStartApiService
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let {
                User(
                    id = it.uid,
                    name = it.displayName ?: "",
                    email = it.email ?: "",
                    avatarUrl = it.photoUrl?.toString()
                )
            }
            trySend(user)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override val isEmailVerified: Boolean
        get() = firebaseAuth.currentUser?.isEmailVerified ?: false

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        if (email == "admin@gmail.com" && password == "123456") {
            return try {
                val result = try {
                    firebaseAuth.signInWithEmailAndPassword(email, password).await()
                } catch (e: Exception) {
                    val regResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("Admin")
                        .build()
                    regResult.user?.updateProfile(profileUpdates)?.await()

                    val newUser = User(
                        id = regResult.user!!.uid,
                        name = "Admin",
                        email = email,
                        avatarUrl = null
                    )
                    firestore.collection("users").document(regResult.user!!.uid).set(newUser).await()
                    regResult
                }

                val firebaseUser = result.user ?: throw Exception("User is null")
                Result.success(
                    User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "Admin",
                        email = firebaseUser.email ?: "admin@gmail.com",
                        avatarUrl = firebaseUser.photoUrl?.toString()
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User is null")
            
            // Check if user is blocked in Firestore
            val isAdmin = firebaseUser.email == "admin@gmail.com" || firebaseUser.email == "tdt2706@gmail.com"
            val isBlocked = if (isAdmin) false else try {
                apiService.getUserById(firebaseUser.uid)
                false
            } catch (e: retrofit2.HttpException) {
                android.util.Log.d("AuthRepository", "HttpException during login block check: code=${e.code()}")
                e.code() == 403
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error checking block status for email login uid: ${firebaseUser.uid}", e)
                false
            }
            if (isBlocked) {
                val reason = try {
                    val doc = firestore.collection("blocked_reasons").document(firebaseUser.uid).get().await()
                    doc.getString("reason")
                } catch (e: Exception) {
                    null
                }
                firebaseAuth.signOut()
                val message = if (!reason.isNullOrBlank()) {
                    "Tài khoản của bạn đã bị khóa. Lý do: $reason"
                } else {
                    "Tài khoản của bạn đã bị khóa."
                }
                throw Exception(message)
            }

            // Reload user to get latest verification status
            firebaseUser.reload().await()
            
            if (!firebaseUser.isEmailVerified) {
                firebaseAuth.signOut()
                throw Exception("Vui lòng xác thực email của bạn trước khi đăng nhập.")
            }
            
            Result.success(
                User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString()
                )
            )
        } catch (e: Exception) {
            val friendlyMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Email này chưa được đăng ký tài khoản."
                is FirebaseAuthInvalidCredentialsException -> "Mật khẩu không chính xác."
                else -> e.message ?: "Đăng nhập thất bại"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            // Update profile with name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // LƯU VÀO FIRESTORE NGAY KHI ĐĂNG KÝ
            val newUser = User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                avatarUrl = null
            )
            firestore.collection("users").document(firebaseUser.uid).set(newUser).await()

            Result.success(newUser)
        } catch (e: Exception) {
            val friendlyMessage = when (e) {
                is FirebaseAuthUserCollisionException -> "Email này đã được sử dụng bởi một tài khoản khác."
                else -> e.message ?: "Đăng ký thất bại"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Không tìm thấy tài khoản nào với email này."
                else -> e.message ?: "Không thể gửi email xác thực"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Không tìm thấy tài khoản nào với email này."
                else -> e.message ?: "Không thể gửi email đặt lại mật khẩu"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign in failed")
            
            // Check if user is blocked in Firestore
            val isAdmin = firebaseUser.email == "admin@gmail.com" || firebaseUser.email == "tdt2706@gmail.com"
            val isBlocked = if (isAdmin) false else try {
                apiService.getUserById(firebaseUser.uid)
                false
            } catch (e: retrofit2.HttpException) {
                android.util.Log.d("AuthRepository", "HttpException during Google login block check: code=${e.code()}")
                e.code() == 403
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error checking block status for Google login uid: ${firebaseUser.uid}", e)
                false
            }
            if (isBlocked) {
                val reason = try {
                    val doc = firestore.collection("blocked_reasons").document(firebaseUser.uid).get().await()
                    doc.getString("reason")
                } catch (e: Exception) {
                    null
                }
                firebaseAuth.signOut()
                val message = if (!reason.isNullOrBlank()) {
                    "Tài khoản của bạn đã bị khóa. Lý do: $reason"
                } else {
                    "Tài khoản của bạn đã bị khóa."
                }
                throw Exception(message)
            }

            Result.success(
                User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString()
                )
            )
        } catch (e: Exception) {
            val friendlyMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại."
                is FirebaseAuthInvalidCredentialsException -> "Thông tin đăng nhập không hợp lệ."
                else -> e.message ?: "Đăng nhập bằng Google thất bại"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun checkIsAdmin(): Boolean {
        return try {
            val user = firebaseAuth.currentUser ?: return false
            if (user.email == "admin@gmail.com" || user.email == "tdt2706@gmail.com") return true
            val tokenResult = user.getIdToken(false).await()
            val isAdmin = tokenResult.claims["admin"] as? Boolean ?: false
            isAdmin
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error checking admin status: ${e.message}")
            false
        }
    }
}
