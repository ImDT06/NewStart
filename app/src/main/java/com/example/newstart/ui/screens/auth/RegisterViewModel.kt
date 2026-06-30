package com.example.newstart.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val acceptTerms: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val registerResult: Resource<Unit>? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onFullNameChange(newValue: String) {
        _uiState.update { it.copy(fullName = newValue, fullNameError = null) }
    }

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, emailError = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { state ->
            val mismatchError = if (state.confirmPassword.isNotEmpty() && newValue != state.confirmPassword) {
                "Mật khẩu không khớp"
            } else null
            state.copy(password = newValue, passwordError = null, confirmPasswordError = mismatchError)
        }
    }

    fun onConfirmPasswordChange(newValue: String) {
        _uiState.update { state ->
            val mismatchError = if (newValue != state.password) "Mật khẩu không khớp" else null
            state.copy(confirmPassword = newValue, confirmPasswordError = mismatchError)
        }
    }

    fun onAcceptTermsChange(newValue: Boolean) {
        _uiState.update { it.copy(acceptTerms = newValue) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun register() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, registerResult = Resource.Loading()) }
            
            val result = authRepository.registerWithEmail(
                _uiState.value.fullName, 
                _uiState.value.email, 
                _uiState.value.password
            )
            
            if (result.isSuccess) {
                authRepository.sendEmailVerification()
                authRepository.logout() // Đăng xuất để tránh race condition tự động đăng nhập
                _uiState.update { it.copy(isLoading = false, registerResult = Resource.Success(Unit)) }
            } else {
                _uiState.update { it.copy(
                    isLoading = false, 
                    registerResult = Resource.Error(result.exceptionOrNull()?.message ?: "Đăng ký thất bại")
                ) }
            }
        }
    }

    fun clearRegisterResult() {
        _uiState.update { it.copy(registerResult = null) }
    }

    private fun validate(): Boolean {
        var isValid = true
        val state = _uiState.value

        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Họ tên không được để trống") }
            isValid = false
        }

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email không được để trống") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            isValid = false
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mật khẩu phải có ít nhất 6 ký tự") }
            isValid = false
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Mật khẩu không khớp") }
            isValid = false
        }

        if (!state.acceptTerms) {
            _uiState.update { it.copy(registerResult = Resource.Error("Bạn cần đồng ý với Điều khoản dịch vụ & Chính sách bảo mật")) }
            isValid = false
        }

        return isValid
    }
}
