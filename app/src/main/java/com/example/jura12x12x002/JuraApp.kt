package com.example.jura12x12x002

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.jura12x12x002.di.AppContainer
import com.example.jura12x12x002.di.LocalAppContainer
import com.example.jura12x12x002.di.appViewModelFactory
import com.example.jura12x12x002.navigation.AppRoute
import com.example.jura12x12x002.ui.AppViewModel
import com.example.jura12x12x002.ui.auth.AuthScreen
import com.example.jura12x12x002.ui.events.EventDetailsScreen
import com.example.jura12x12x002.ui.events.MainScreen

@Composable
fun JuraApp() {
    val container = remember { AppContainer() }

    CompositionLocalProvider(LocalAppContainer provides container) {
        val appViewModel: AppViewModel = viewModel(
            factory = appViewModelFactory(container)
        )
        val appState by appViewModel.uiState.collectAsStateWithLifecycle()

        val startRoute = if (appState.isLoggedIn) AppRoute.Events else AppRoute.Auth
        val backStack = rememberNavBackStack(startRoute)

        LaunchedEffect(appState.isLoggedIn) {
            val rootRoute = if (appState.isLoggedIn) AppRoute.Events else AppRoute.Auth
            if (backStack.firstOrNull() != rootRoute) {
                backStack.clear()
                backStack.add(rootRoute)
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<AppRoute.Auth> {
                    AuthScreen()
                }

                entry<AppRoute.Events> {
                    MainScreen(
                        onOpenEvent = { eventId ->
                            backStack.add(AppRoute.EventDetails(eventId))
                        }
                    )
                }

                entry<AppRoute.EventDetails> { route ->
                    EventDetailsScreen(
                        eventId = route.eventId,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        }
                    )
                }
            }
        )
    }
}
