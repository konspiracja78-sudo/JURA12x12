package com.example.jura12x12x002.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) }

    if (isRegisterMode) {
        RegisterScreen(
            onRegisterSuccess = onAuthSuccess,
            onGoToLogin = { isRegisterMode = false }
        )
    } else {
        LoginScreen(
            onLoginSuccess = onAuthSuccess,
            onGoToRegister = { isRegisterMode = true }
        )
    }
}
