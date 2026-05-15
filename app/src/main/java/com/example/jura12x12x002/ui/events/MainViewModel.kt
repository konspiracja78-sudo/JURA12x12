package com.example.jura12x12x002.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jura12x12x002.R
import com.example.jura12x12x002.domain.usecase.auth.LogoutUseCase
import com.example.jura12x12x002.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.jura12x12x002.domain.usecase.event.AddEventUseCase
import com.example.jura12x12x002.domain.usecase.event.ObserveEventsUseCase
import com.example.jura12x12x002.model.EVENT_STATUS_ACTIVE
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.ui.UiText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val allEvents: List<Event> = emptyList(),
    val currentUserEmail: String = "",
    val isLoading: Boolean = true,
    val isAddingEvent: Boolean = false,
    val selectedFilterIndex: Int = 0,
    val searchQuery: String = ""
)

sealed interface MainUiEffect {
    data class ShowMessage(val message: UiText) : MainUiEffect
    data object EventSaved : MainUiEffect
}

class MainViewModel(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    observeEventsUseCase: ObserveEventsUseCase,
    private val addEventUseCase: AddEventUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MainUiEffect>()
    val effects: SharedFlow<MainUiEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { email ->
                _uiState.update { state ->
                    state.copy(currentUserEmail = email.orEmpty())
                }
            }
        }

        viewModelScope.launch {
            observeEventsUseCase()
                .catch { error ->
                    _uiState.update { state -> state.copy(isLoading = false) }
                    emitEffect(
                        MainUiEffect.ShowMessage(
                            UiText.StringResource(
                                R.string.main_msg_fetch_error,
                                error.message ?: UiText.StringResource(R.string.common_unknown_error)
                            )
                        )
                    )
                }
                .collect { events ->
                    _uiState.update { state ->
                        state.copy(
                            allEvents = events,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { state -> state.copy(searchQuery = value) }
    }

    fun onFilterSelected(index: Int) {
        _uiState.update { state -> state.copy(selectedFilterIndex = index) }
    }

    fun addEvent(event: Event) {
        val state = uiState.value
        if (state.isAddingEvent) return

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isAddingEvent = true) }
            val eventToSave = event.copy(
                id = "",
                authorEmail = state.currentUserEmail,
                status = EVENT_STATUS_ACTIVE,
                statusReason = ""
            )

            runCatching {
                addEventUseCase(eventToSave)
            }.onSuccess {
                emitEffect(MainUiEffect.ShowMessage(UiText.StringResource(R.string.main_msg_event_added)))
                emitEffect(MainUiEffect.EventSaved)
            }.onFailure { error ->
                emitEffect(
                    MainUiEffect.ShowMessage(
                        UiText.StringResource(
                            R.string.main_msg_add_event_error,
                            error.message ?: UiText.StringResource(R.string.common_unknown_error)
                        )
                    )
                )
            }
            _uiState.update { current -> current.copy(isAddingEvent = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching {
                logoutUseCase()
            }.onFailure { error ->
                emitEffect(
                    MainUiEffect.ShowMessage(
                        UiText.StringResource(
                            R.string.main_msg_logout_error,
                            error.message ?: UiText.StringResource(R.string.common_try_again)
                        )
                    )
                )
            }
        }
    }

    private suspend fun emitEffect(effect: MainUiEffect) {
        _effects.emit(effect)
    }
}
