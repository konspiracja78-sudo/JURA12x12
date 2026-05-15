package com.example.jura12x12x002.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.R

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onGoToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.auth_register_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Start)
        )

        OutlinedTextField(
            value = state.registerEmail,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.common_email)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.registerPassword,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.common_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.registerRepeatPassword,
            onValueChange = onRepeatPasswordChange,
            label = { Text(stringResource(R.string.common_repeat_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text(stringResource(R.string.auth_register_button))
        }

        TextButton(
            onClick = onGoToLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text(stringResource(R.string.auth_go_to_login))
        }
    }
}
