package com.example.jura12x12x002.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.jura12x12x002.ui.AppViewModel
import com.example.jura12x12x002.ui.auth.AuthViewModel
import com.example.jura12x12x002.ui.events.EventDetailsViewModel
import com.example.jura12x12x002.ui.events.MainViewModel

fun appViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        AppViewModel(
            initialCurrentUserEmail = container.initialCurrentUserEmail,
            observeAuthStateUseCase = container.observeAuthStateUseCase
        )
    }
}

fun authViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        AuthViewModel(
            loginUseCase = container.loginUseCase,
            registerUseCase = container.registerUseCase,
            resetPasswordUseCase = container.resetPasswordUseCase
        )
    }
}

fun mainViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        MainViewModel(
            observeAuthStateUseCase = container.observeAuthStateUseCase,
            observeEventsUseCase = container.observeEventsUseCase,
            addEventUseCase = container.addEventUseCase,
            logoutUseCase = container.logoutUseCase
        )
    }
}

fun eventDetailsViewModelFactory(
    container: AppContainer,
    eventId: String
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        EventDetailsViewModel(
            eventId = eventId,
            observeAuthStateUseCase = container.observeAuthStateUseCase,
            observeEventUseCase = container.observeEventUseCase,
            observeEventMessagesUseCase = container.observeEventMessagesUseCase,
            updateEventUseCase = container.updateEventUseCase,
            cancelEventUseCase = container.cancelEventUseCase,
            restoreEventUseCase = container.restoreEventUseCase,
            toggleEventParticipationUseCase = container.toggleEventParticipationUseCase,
            sendEventMessageUseCase = container.sendEventMessageUseCase
        )
    }
}
