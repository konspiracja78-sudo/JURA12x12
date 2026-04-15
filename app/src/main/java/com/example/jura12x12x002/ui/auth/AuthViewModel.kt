package com.example.jura12x12x002.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jura12x12x002.domain.usecase.auth.LoginUseCase
import com.example.jura12x12x002.domain.usecase.auth.RegisterUseCase
import com.example.jura12x12x002.domain.usecase.auth.ResetPasswordUseCase
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

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

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
            sendMessage("Uzupełnij email i hasło")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                loginUseCase(email, password)
            }.onSuccess {
                sendMessage("Zalogowano")
            }.onFailure { error ->
                sendMessage("Błąd logowania: ${error.message ?: "spróbuj ponownie"}")
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun register() {
        val email = sanitizeEmail(uiState.value.registerEmail)
        val password = uiState.value.registerPassword
        val repeatPassword = uiState.value.registerRepeatPassword

        if (email.isBlank() || password.isBlank() || repeatPassword.isBlank()) {
            sendMessage("Uzupełnij wszystkie pola")
            return
        }

        if (password != repeatPassword) {
            sendMessage("Hasła nie są takie same")
            return
        }

        if (password.length < 6) {
            sendMessage("Hasło musi mieć co najmniej 6 znaków")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                registerUseCase(email, password)
            }.onSuccess {
                sendMessage("Konto utworzone")
            }.onFailure { error ->
                sendMessage("Błąd rejestracji: ${error.message ?: "spróbuj ponownie"}")
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun resetPassword() {
        val email = sanitizeEmail(uiState.value.loginEmail)

        if (email.isBlank()) {
            sendMessage("Najpierw wpisz email")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                resetPasswordUseCase(email)
            }.onSuccess {
                sendMessage("Wysłano mail do resetu hasła")
            }.onFailure { error ->
                sendMessage("Błąd resetu hasła: ${error.message ?: "spróbuj ponownie"}")
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
