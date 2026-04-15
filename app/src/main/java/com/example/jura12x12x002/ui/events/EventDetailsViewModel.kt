package com.example.jura12x12x002.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jura12x12x002.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.jura12x12x002.domain.usecase.event.CancelEventUseCase
import com.example.jura12x12x002.domain.usecase.event.ObserveEventMessagesUseCase
import com.example.jura12x12x002.domain.usecase.event.ObserveEventUseCase
import com.example.jura12x12x002.domain.usecase.event.RestoreEventUseCase
import com.example.jura12x12x002.domain.usecase.event.SendEventMessageUseCase
import com.example.jura12x12x002.domain.usecase.event.ToggleEventParticipationUseCase
import com.example.jura12x12x002.domain.usecase.event.UpdateEventUseCase
import com.example.jura12x12x002.model.ChatMessage
import com.example.jura12x12x002.model.Event
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailsUiState(
    val currentEvent: Event? = null,
    val messages: List<ChatMessage> = emptyList(),
    val currentUserEmail: String = "",
    val newMessage: String = "",
    val isEventLoading: Boolean = true,
    val isEditingEvent: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isUpdatingStatus: Boolean = false
)

sealed interface EventDetailsUiEffect {
    data class ShowMessage(val message: String) : EventDetailsUiEffect
    data object CloseEditDialog : EventDetailsUiEffect
    data object CloseCancelDialog : EventDetailsUiEffect
}

class EventDetailsViewModel(
    private val eventId: String,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    observeEventUseCase: ObserveEventUseCase,
    observeEventMessagesUseCase: ObserveEventMessagesUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val cancelEventUseCase: CancelEventUseCase,
    private val restoreEventUseCase: RestoreEventUseCase,
    private val toggleEventParticipationUseCase: ToggleEventParticipationUseCase,
    private val sendEventMessageUseCase: SendEventMessageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventDetailsUiState())
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EventDetailsUiEffect>()
    val effects: SharedFlow<EventDetailsUiEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { email ->
                _uiState.update { state ->
                    state.copy(currentUserEmail = email.orEmpty())
                }
            }
        }

        viewModelScope.launch {
            observeEventUseCase(eventId)
                .catch { error ->
                    _uiState.update { state -> state.copy(isEventLoading = false) }
                    emitEffect(
                        EventDetailsUiEffect.ShowMessage(
                            "Błąd wydarzenia: ${error.message ?: "nieznany błąd"}"
                        )
                    )
                }
                .collect { event ->
                    _uiState.update { state ->
                        state.copy(
                            currentEvent = event,
                            isEventLoading = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            observeEventMessagesUseCase(eventId)
                .catch { error ->
                    emitEffect(
                        EventDetailsUiEffect.ShowMessage(
                            "Błąd czatu: ${error.message ?: "nieznany błąd"}"
                        )
                    )
                }
                .collect { messages ->
                    _uiState.update { state -> state.copy(messages = messages) }
                }
        }
    }

    fun onMessageChange(value: String) {
        _uiState.update { state -> state.copy(newMessage = value) }
    }

    fun sendMessage() {
        val state = uiState.value
        val event = state.currentEvent

        if (event == null) {
            emitMessage("Nie udało się wczytać wydarzenia")
            return
        }

        if (state.isSendingMessage) return

        if (event.id.isBlank()) {
            emitMessage("Brak ID wydarzenia")
            return
        }

        if (state.newMessage.isBlank()) {
            emitMessage("Wpisz wiadomość")
            return
        }

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isSendingMessage = true) }
            runCatching {
                sendEventMessageUseCase(
                    event.id,
                    ChatMessage(
                        text = state.newMessage.trim(),
                        authorEmail = state.currentUserEmail.ifBlank { "Brak emaila" },
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                _uiState.update { current ->
                    current.copy(
                        isSendingMessage = false,
                        newMessage = ""
                    )
                }
            }.onFailure { error ->
                _uiState.update { current -> current.copy(isSendingMessage = false) }
                emitEffect(
                    EventDetailsUiEffect.ShowMessage(
                        "Błąd wysyłki: ${error.message ?: "nieznany błąd"}"
                    )
                )
            }
        }
    }

    fun toggleParticipation() {
        val state = uiState.value
        val event = state.currentEvent

        if (event == null) {
            emitMessage("Nie udało się wczytać wydarzenia")
            return
        }

        if (state.currentUserEmail.isBlank()) {
            emitMessage("Brak zalogowanego emaila")
            return
        }

        val isJoined = event.participantEmails.contains(state.currentUserEmail)

        viewModelScope.launch {
            runCatching {
                toggleEventParticipationUseCase(
                    event.id,
                    state.currentUserEmail,
                    isJoined
                )
            }.onSuccess {
                emitEffect(
                    EventDetailsUiEffect.ShowMessage(
                        if (isJoined) {
                            "Opuściłeś wydarzenie"
                        } else {
                            "Dołączyłeś do wydarzenia"
                        }
                    )
                )
            }.onFailure { error ->
                emitEffect(
                    EventDetailsUiEffect.ShowMessage(
                        "Błąd zapisu uczestnika: ${error.message ?: "nieznany błąd"}"
                    )
                )
            }
        }
    }

    fun updateEvent(updatedEvent: Event) {
        val state = uiState.value
        if (state.isEditingEvent) return

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isEditingEvent = true) }
            runCatching {
                updateEventUseCase(updatedEvent)
            }.onSuccess {
                emitEffect(EventDetailsUiEffect.CloseEditDialog)
                emitEffect(EventDetailsUiEffect.ShowMessage("Zapisano zmiany"))
            }.onFailure { error ->
                emitEffect(
                    EventDetailsUiEffect.ShowMessage(
                        "Błąd edycji: ${error.message ?: "nieznany błąd"}"
                    )
                )
            }
            _uiState.update { current -> current.copy(isEditingEvent = false) }
        }
    }

    fun cancelEvent(reason: String) {
        val state = uiState.value
        val event = state.currentEvent

        if (event == null) {
            emitMessage("Nie udało się wczytać wydarzenia")
            return
        }

        if (state.isUpdatingStatus) return

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isUpdatingStatus = true) }
            runCatching {
                cancelEventUseCase(event.id, reason.trim())
            }.onSuccess {
                emitEffect(EventDetailsUiEffect.CloseCancelDialog)
                emitEffect(EventDetailsUiEffect.ShowMessage("Wydarzenie zostało odwołane"))
            }.onFailure { error ->
                emitEffect(
                    EventDetailsUiEffect.ShowMessage(
                        "Błąd odwołania: ${error.message ?: "nieznany błąd"}"
                    )
                )
            }
            _uiState.update { current -> current.copy(isUpdatingStatus = false) }
        }
    }

    fun restoreEvent() {
        val state = uiState.value
        val event = state.currentEvent

        if (event == null) {
            emitMessage("Nie udało się wczytać wydarzenia")
            return
        }

        if (state.isUpdatingStatus) return

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isUpdatingStatus = true) }
            runCatching {
                restoreEventUseCase(event.id)
            }.onSuccess {
                emitEffect(EventDetailsUiEffect.ShowMessage("Przywrócono wydarzenie"))
            }.onFailure { error ->
                emitEffect(
                    EventDetailsUiEffect.ShowMessage(
                        "Błąd przywracania: ${error.message ?: "nieznany błąd"}"
                    )
                )
            }
            _uiState.update { current -> current.copy(isUpdatingStatus = false) }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            emitEffect(EventDetailsUiEffect.ShowMessage(message))
        }
    }

    private suspend fun emitEffect(effect: EventDetailsUiEffect) {
        _effects.emit(effect)
    }
}
