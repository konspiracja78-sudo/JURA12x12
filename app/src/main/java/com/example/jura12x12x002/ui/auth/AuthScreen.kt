package com.example.jura12x12x002.ui.auth

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jura12x12x002.di.LocalAppContainer
import com.example.jura12x12x002.di.authViewModelFactory
import com.example.jura12x12x002.ui.asString

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val viewModel: AuthViewModel = viewModel(
        factory = authViewModelFactory(container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message.asString(context), Toast.LENGTH_LONG).show()
        }
    }

    if (uiState.isRegisterMode) {
        RegisterScreen(
            state = uiState,
            onEmailChange = viewModel::onRegisterEmailChange,
            onPasswordChange = viewModel::onRegisterPasswordChange,
            onRepeatPasswordChange = viewModel::onRegisterRepeatPasswordChange,
            onRegister = viewModel::register,
            onGoToLogin = viewModel::showLogin
        )
    } else {
        LoginScreen(
            state = uiState,
            onEmailChange = viewModel::onLoginEmailChange,
            onPasswordChange = viewModel::onLoginPasswordChange,
            onLogin = viewModel::login,
            onResetPassword = viewModel::resetPassword,
            onGoToRegister = viewModel::showRegister
        )
    }
}
