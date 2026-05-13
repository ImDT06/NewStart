package com.example.newstart.ui.screens.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.newstart.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var mobile by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var acceptTerms by mutableStateOf(value = false)
    var passwordVisible by mutableStateOf(value = false)

    // Trạng thái lỗi
    var fullNameError by mutableStateOf<String?>(null)
    var emailError by mutableStateOf<String?>(null)
    var mobileError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var confirmPasswordError by mutableStateOf<String?>(null)

    fun onFullNameChange(newValue: String) {
        fullName = newValue
        fullNameError = null
    }

    fun onEmailChange(newValue: String) {
        email = newValue
        emailError = null
    }

    fun onMobileChange(newValue: String) {
        mobile = newValue
        mobileError = null
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
        passwordError = null
    }

    fun onConfirmPasswordChange(newValue: String) {
        confirmPassword = newValue
        confirmPasswordError = null
    }

    fun onAcceptTermsChange(newValue: Boolean) {
        acceptTerms = newValue
    }

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    fun validateAndRegister(): Boolean {
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

        if (mobile.isBlank()) {
            mobileError = context.getString(R.string.error_mobile_empty)
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
            // Có thể thêm thông báo lỗi cho checkbox nếu cần
            isValid = false
        }

        return isValid
    }
}
