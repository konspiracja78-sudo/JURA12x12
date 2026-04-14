package com.example.jura12x12x002

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.jura12x12x002.ui.auth.AuthScreen
import com.example.jura12x12x002.ui.events.MainScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun JuraApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    if (isLoggedIn) {
        MainScreen(
            onLogout = {
                auth.signOut()
                isLoggedIn = false
            }
        )
    } else {
        AuthScreen(
            onAuthSuccess = {
                isLoggedIn = true
            }
        )
    }
}
