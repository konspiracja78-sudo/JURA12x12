package com.example.jura12x12x002.data.repository

import com.example.jura12x12x002.data.firebase.awaitResult
import com.example.jura12x12x002.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {
    override fun observeCurrentUserEmail(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.email)
        }

        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.email)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun login(email: String, password: String) {
        auth.signOut()
        auth.signInWithEmailAndPassword(email, password).awaitResult()
    }

    override suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).awaitResult()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).awaitResult()
    }

    override suspend fun logout() {
        auth.signOut()
    }
}
