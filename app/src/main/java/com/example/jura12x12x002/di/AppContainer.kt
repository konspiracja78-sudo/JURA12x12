package com.example.jura12x12x002.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.jura12x12x002.data.repository.FirebaseAuthRepository
import com.example.jura12x12x002.data.repository.FirebaseEventRepository
import com.example.jura12x12x002.domain.repository.AuthRepository
import com.example.jura12x12x002.domain.repository.EventRepository
import com.example.jura12x12x002.domain.usecase.auth.LoginUseCase
import com.example.jura12x12x002.domain.usecase.auth.LogoutUseCase
import com.example.jura12x12x002.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.jura12x12x002.domain.usecase.auth.RegisterUseCase
import com.example.jura12x12x002.domain.usecase.auth.ResetPasswordUseCase
import com.example.jura12x12x002.domain.usecase.event.AddEventUseCase
import com.example.jura12x12x002.domain.usecase.event.CancelEventUseCase
import com.example.jura12x12x002.domain.usecase.event.ObserveEventMessagesUseCase
import com.example.jura12x12x002.domain.usecase.event.ObserveEventUseCase
import com.example.jura12x12x002.domain.usecase.event.ObserveEventsUseCase
import com.example.jura12x12x002.domain.usecase.event.RestoreEventUseCase
import com.example.jura12x12x002.domain.usecase.event.SendEventMessageUseCase
import com.example.jura12x12x002.domain.usecase.event.ToggleEventParticipationUseCase
import com.example.jura12x12x002.domain.usecase.event.UpdateEventUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer {
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val initialCurrentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    private val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(firebaseAuth)
    }

    private val eventRepository: EventRepository by lazy {
        FirebaseEventRepository(firestore)
    }

    val observeAuthStateUseCase by lazy { ObserveAuthStateUseCase(authRepository) }
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository) }
    val resetPasswordUseCase by lazy { ResetPasswordUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }

    val observeEventsUseCase by lazy { ObserveEventsUseCase(eventRepository) }
    val observeEventUseCase by lazy { ObserveEventUseCase(eventRepository) }
    val observeEventMessagesUseCase by lazy { ObserveEventMessagesUseCase(eventRepository) }
    val addEventUseCase by lazy { AddEventUseCase(eventRepository) }
    val updateEventUseCase by lazy { UpdateEventUseCase(eventRepository) }
    val cancelEventUseCase by lazy { CancelEventUseCase(eventRepository) }
    val restoreEventUseCase by lazy { RestoreEventUseCase(eventRepository) }
    val toggleEventParticipationUseCase by lazy { ToggleEventParticipationUseCase(eventRepository) }
    val sendEventMessageUseCase by lazy { SendEventMessageUseCase(eventRepository) }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
