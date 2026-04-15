package com.example.jura12x12x002.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jura12x12x002.domain.usecase.auth.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val currentUserEmail: String? = null
) {
    val isLoggedIn: Boolean
        get() = currentUserEmail != null
}

class AppViewModel(
    initialCurrentUserEmail: String?,
    observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(initialCurrentUserEmail))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { email ->
                _uiState.update { state ->
                    state.copy(currentUserEmail = email)
                }
            }
        }
    }
}
