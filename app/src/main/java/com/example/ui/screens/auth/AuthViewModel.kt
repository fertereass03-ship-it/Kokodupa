package com.example.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.util.AvatarManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthTab {
    LOGIN,
    REGISTER
}

data class AuthUiState(
    val selectedTab: AuthTab = AuthTab.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val avatarUrl: String = AvatarManager.APP_ICON_AVATAR,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val isResetLoading: Boolean = false,
    val resetMessage: String? = null,
    val resetErrorMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setTab(tab: AuthTab) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null, successMessage = null) }
    }

    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun setConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun setDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null) }
    }

    fun setAvatarUrl(url: String) {
        _uiState.update { it.copy(avatarUrl = url) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun setForgotPasswordDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = show,
                forgotPasswordEmail = if (show) it.email else "",
                resetMessage = null,
                resetErrorMessage = null
            )
        }
    }

    fun setForgotPasswordEmail(email: String) {
        _uiState.update { it.copy(forgotPasswordEmail = email, resetErrorMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password.trim()

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Введите email") }
            return
        }
        if (!email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Некорректный формат email") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Введите пароль") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.loginWithEmail(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onFailure { ex ->
                val msg = ex.message ?: "Ошибка авторизации. Проверьте данные."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        val name = state.displayName.trim()
        val email = state.email.trim()
        val password = state.password.trim()
        val confirm = state.confirmPassword.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Введите имя пользователя (никнейм)") }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Введите email") }
            return
        }
        if (!email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Некорректный формат email") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Пароль должен содержать минимум 6 символов") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(errorMessage = "Пароли не совпадают") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.registerWithEmail(
                email = email,
                password = password,
                displayName = name,
                avatarUrl = state.avatarUrl.ifBlank { AvatarManager.APP_ICON_AVATAR }
            )
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onFailure { ex ->
                val msg = ex.message ?: "Ошибка при регистрации"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun loginWithGoogle(
        email: String,
        displayName: String,
        avatarUrl: String? = null,
        onSuccess: () -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val finalAvatar = avatarUrl?.takeIf { it.isNotBlank() } ?: AvatarManager.APP_ICON_AVATAR
            val result = authRepository.loginWithGoogle(email, displayName, finalAvatar)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Не удалось войти через Google"
                    )
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.forgotPasswordEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(resetErrorMessage = "Введите корректный email") }
            return
        }

        _uiState.update { it.copy(isResetLoading = true, resetErrorMessage = null, resetMessage = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isResetLoading = false,
                        resetMessage = "Инструкции по восстановлению отправлены на $email"
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isResetLoading = false,
                        resetErrorMessage = ex.message ?: "Не удалось отправить запрос на восстановление"
                    )
                }
            }
        }
    }
}
