package com.example.jura12x12x002

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.jura12x12x002.navigation.AppRoute
import com.example.jura12x12x002.ui.auth.AuthScreen
import com.example.jura12x12x002.ui.events.EventDetailsScreen
import com.example.jura12x12x002.ui.events.MainScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun JuraApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val startRoute = if (auth.currentUser != null) {
        AppRoute.Events
    } else {
        AppRoute.Auth
    }
    val backStack = rememberNavBackStack(startRoute)

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
                AuthScreen(
                    onAuthSuccess = {
                        backStack.clear()
                        backStack.add(AppRoute.Events)
                    }
                )
            }

            entry<AppRoute.Events> {
                MainScreen(
                    onLogout = {
                        auth.signOut()
                        backStack.clear()
                        backStack.add(AppRoute.Auth)
                    },
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
