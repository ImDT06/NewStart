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

data class LoginUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loginResult: Resource<Unit>? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, emailError = null, loginResult = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, passwordError = null, loginResult = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginResult = Resource.Loading()) }
            
            val result = authRepository.loginWithEmail(_uiState.value.email, _uiState.value.password)
            
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    loginResult = if (result.isSuccess) Resource.Success(Unit) 
                                 else Resource.Error(result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
                )
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginResult = Resource.Loading()) }
            val result = authRepository.loginWithGoogle(idToken)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    loginResult = if (result.isSuccess) Resource.Success(Unit)
                                 else Resource.Error(result.exceptionOrNull()?.message ?: "Đăng nhập Google thất bại")
                )
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onError("Vui lòng nhập email hợp lệ")
            return
        }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Gửi yêu cầu thất bại") }
        }
    }

    fun clearLoginResult() {
        _uiState.update { it.copy(loginResult = null) }
    }

    private fun validate(): Boolean {
        var isValid = true
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email không được để trống") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            isValid = false
        }

        if (password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mật khẩu phải có ít nhất 6 ký tự") }
            isValid = false
        }
        
        return isValid
    }
}
