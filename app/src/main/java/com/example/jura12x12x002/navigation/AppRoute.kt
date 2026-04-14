package com.example.jura12x12x002.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppRoute : NavKey {
    @Serializable
    data object Auth : AppRoute

    @Serializable
    data object Events : AppRoute

    @Serializable
    data class EventDetails(val eventId: String) : AppRoute
}
