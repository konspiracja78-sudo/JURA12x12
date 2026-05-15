package com.example.jura12x12x002.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jura12x12x002.R
import com.example.jura12x12x002.domain.usecase.auth.LoginUseCase
import com.example.jura12x12x002.domain.usecase.auth.RegisterUseCase
import com.example.jura12x12x002.domain.usecase.auth.ResetPasswordUseCase
import com.example.jura12x12x002.ui.UiText
import com.example.jura12x12x002.utils.sanitizeEmail
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isRegisterMode: Boolean = false,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerRepeatPassword: String = "",
    val isLoading: Boolean = false
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>()
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    fun showRegister() {
        _uiState.update { it.copy(isRegisterMode = true) }
    }

    fun showLogin() {
        _uiState.update { it.copy(isRegisterMode = false) }
    }

    fun onLoginEmailChange(value: String) {
        _uiState.update { it.copy(loginEmail = value) }
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.update { it.copy(loginPassword = value) }
    }

    fun onRegisterEmailChange(value: String) {
        _uiState.update { it.copy(registerEmail = value) }
    }

    fun onRegisterPasswordChange(value: String) {
        _uiState.update { it.copy(registerPassword = value) }
    }

    fun onRegisterRepeatPasswordChange(value: String) {
        _uiState.update { it.copy(registerRepeatPassword = value) }
    }

    fun login() {
        val email = sanitizeEmail(uiState.value.loginEmail)
        val password = uiState.value.loginPassword

        if (email.isBlank() || password.isBlank()) {
            sendMessage(UiText.StringResource(R.string.auth_msg_fill_email_password))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                loginUseCase(email, password)
            }.onSuccess {
                sendMessage(UiText.StringResource(R.string.auth_msg_logged_in))
            }.onFailure { error ->
                sendMessage(
                    UiText.StringResource(
                        R.string.auth_msg_login_error,
                        error.message ?: UiText.StringResource(R.string.common_try_again)
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun register() {
        val email = sanitizeEmail(uiState.value.registerEmail)
        val password = uiState.value.registerPassword
        val repeatPassword = uiState.value.registerRepeatPassword

        if (email.isBlank() || password.isBlank() || repeatPassword.isBlank()) {
            sendMessage(UiText.StringResource(R.string.auth_msg_fill_all_fields))
            return
        }

        if (password != repeatPassword) {
            sendMessage(UiText.StringResource(R.string.auth_msg_passwords_not_match))
            return
        }

        if (password.length < 6) {
            sendMessage(UiText.StringResource(R.string.auth_msg_password_too_short))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                registerUseCase(email, password)
            }.onSuccess {
                sendMessage(UiText.StringResource(R.string.auth_msg_account_created))
            }.onFailure { error ->
                sendMessage(
                    UiText.StringResource(
                        R.string.auth_msg_register_error,
                        error.message ?: UiText.StringResource(R.string.common_try_again)
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun resetPassword() {
        val email = sanitizeEmail(uiState.value.loginEmail)

        if (email.isBlank()) {
            sendMessage(UiText.StringResource(R.string.auth_msg_enter_email_first))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                resetPasswordUseCase(email)
            }.onSuccess {
                sendMessage(UiText.StringResource(R.string.auth_msg_password_reset_sent))
            }.onFailure { error ->
                sendMessage(
                    UiText.StringResource(
                        R.string.auth_msg_password_reset_error,
                        error.message ?: UiText.StringResource(R.string.common_try_again)
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun sendMessage(message: UiText) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
