package com.example.jura12x12x002.domain.usecase.event

import com.example.jura12x12x002.domain.repository.EventRepository
import com.example.jura12x12x002.model.ChatMessage
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.utils.parseDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveEventsUseCase(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> = eventRepository.observeEvents()
        .map { events -> events.sortedBy(::parseDateTime) }
}

class ObserveEventUseCase(
    private val eventRepository: EventRepository
) {
    operator fun invoke(eventId: String): Flow<Event?> = eventRepository.observeEvent(eventId)
}

class ObserveEventMessagesUseCase(
    private val eventRepository: EventRepository
) {
    operator fun invoke(eventId: String): Flow<List<ChatMessage>> = eventRepository.observeMessages(eventId)
}

class AddEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event) {
        eventRepository.addEvent(event)
    }
}

class UpdateEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event) {
        eventRepository.updateEvent(event)
    }
}

class CancelEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String, reason: String) {
        eventRepository.cancelEvent(eventId, reason)
    }
}

class RestoreEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String) {
        eventRepository.restoreEvent(eventId)
    }
}

class ToggleEventParticipationUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String, userEmail: String, isJoined: Boolean) {
        eventRepository.toggleParticipation(eventId, userEmail, isJoined)
    }
}

class SendEventMessageUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String, message: ChatMessage) {
        eventRepository.sendMessage(eventId, message)
    }
}
