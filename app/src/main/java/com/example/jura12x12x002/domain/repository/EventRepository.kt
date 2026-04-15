package com.example.jura12x12x002.domain.repository

import com.example.jura12x12x002.model.ChatMessage
import com.example.jura12x12x002.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>

    fun observeEvent(eventId: String): Flow<Event?>

    fun observeMessages(eventId: String): Flow<List<ChatMessage>>

    suspend fun addEvent(event: Event)

    suspend fun updateEvent(event: Event)

    suspend fun cancelEvent(eventId: String, reason: String)

    suspend fun restoreEvent(eventId: String)

    suspend fun toggleParticipation(eventId: String, userEmail: String, isJoined: Boolean)

    suspend fun sendMessage(eventId: String, message: ChatMessage)
}
