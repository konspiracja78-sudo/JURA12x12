package com.example.jura12x12x002.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUserEmail(): Flow<String?>

    suspend fun login(email: String, password: String)

    suspend fun register(email: String, password: String)

    suspend fun sendPasswordReset(email: String)

    suspend fun logout()
}
