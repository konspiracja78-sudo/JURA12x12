package com.example.jura12x12x002.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.utils.sanitizeEmail
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isResettingPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Logowanie",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val loginEmail = sanitizeEmail(email)

                if (loginEmail.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Uzupełnij email i hasło", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.signOut()

                auth.signInWithEmailAndPassword(loginEmail, password)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Zalogowano", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Błąd logowania. Sprawdź dokładnie ten sam email i nowe hasło z resetu. Szczegóły: ${e.message ?: "spróbuj ponownie"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zaloguj")
        }

        Button(
            onClick = {
                if (isResettingPassword) return@Button

                val resetEmail = sanitizeEmail(email)

                if (resetEmail.isBlank()) {
                    Toast.makeText(context, "Najpierw wpisz email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isResettingPassword = true

                auth.sendPasswordResetEmail(resetEmail)
                    .addOnSuccessListener {
                        isResettingPassword = false
                        Toast.makeText(
                            context,
                            "Wysłano mail do resetu hasła",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        isResettingPassword = false
                        Toast.makeText(
                            context,
                            "Błąd resetu hasła: ${e.message ?: "spróbuj ponownie"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isResettingPassword) "Wysyłanie..." else "Resetuj hasło")
        }

        TextButton(
            onClick = onGoToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nie masz konta? Przejdź do rejestracji")
        }
    }
}
