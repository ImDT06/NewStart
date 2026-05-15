package com.example.newstart.ui.screens.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.R
import com.example.newstart.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {
    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var acceptTerms by mutableStateOf(value = false)
    
    var passwordVisible by mutableStateOf(false)
    var confirmPasswordVisible by mutableStateOf(false)

    // Trạng thái lỗi
    var fullNameError by mutableStateOf<String?>(null)
    var emailError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var confirmPasswordError by mutableStateOf<String?>(null)

    var isLoading by mutableStateOf(false)
        private set

    fun onFullNameChange(newValue: String) {
        fullName = newValue
        fullNameError = null
    }

    fun onEmailChange(newValue: String) {
        email = newValue
        emailError = null
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
        passwordError = null
        if (confirmPassword.isNotEmpty() && newValue != confirmPassword) {
            confirmPasswordError = context.getString(R.string.error_password_mismatch)
        } else {
            confirmPasswordError = null
        }
    }

    fun onConfirmPasswordChange(newValue: String) {
        confirmPassword = newValue
        if (newValue != password) {
            confirmPasswordError = context.getString(R.string.error_password_mismatch)
        } else {
            confirmPasswordError = null
        }
    }

    fun onAcceptTermsChange(newValue: Boolean) {
        acceptTerms = newValue
    }

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible
    }

    fun register(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!validate()) {
            onError("Vui lòng kiểm tra lại thông tin")
            return
        }

        viewModelScope.launch {
            isLoading = true
            val result = authRepository.registerWithEmail(fullName, email, password)
            
            result.onSuccess {
                authRepository.sendEmailVerification()
                isLoading = false
                onSuccess()
            }.onFailure {
                isLoading = false
                onError(it.message ?: "Đăng ký thất bại")
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true

        if (fullName.isBlank()) {
            fullNameError = context.getString(R.string.error_name_empty)
            isValid = false
        }

        if (email.isBlank()) {
            emailError = context.getString(R.string.error_email_empty)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = context.getString(R.string.error_email_invalid)
            isValid = false
        }

        if (password.length < 6) {
            passwordError = context.getString(R.string.error_password_short)
            isValid = false
        }

        if (password != confirmPassword) {
            confirmPasswordError = context.getString(R.string.error_password_mismatch)
            isValid = false
        }

        if (!acceptTerms) {
            isValid = false
        }

        return isValid
    }
}
