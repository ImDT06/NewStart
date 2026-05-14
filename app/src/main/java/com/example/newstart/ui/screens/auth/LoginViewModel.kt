package com.example.newstart.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    
    var password by mutableStateOf("")
        private set

    var passwordVisible by mutableStateOf(false)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun onEmailChange(newValue: String) {
        email = newValue
        emailError = null
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
        passwordError = null
    }

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!validate()) return

        viewModelScope.launch {
            isLoading = true
            val result = authRepository.loginWithEmail(email, password)
            isLoading = false
            
            result.onSuccess {
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = authRepository.loginWithGoogle(idToken)
            isLoading = false
            
            result.onSuccess {
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Đăng nhập Google thất bại")
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        if (email.isBlank()) {
            emailError = "Email không được để trống"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Email không hợp lệ"
            isValid = false
        }

        if (password.length < 6) {
            passwordError = "Mật khẩu phải có ít nhất 6 ký tự"
            isValid = false
        }
        
        return isValid
    }
}
