package com.example.jura12x12x002.data.repository

import com.example.jura12x12x002.data.firebase.awaitResult
import com.example.jura12x12x002.domain.repository.EventRepository
import com.example.jura12x12x002.model.ChatMessage
import com.example.jura12x12x002.model.EVENT_STATUS_ACTIVE
import com.example.jura12x12x002.model.EVENT_STATUS_CANCELLED
import com.example.jura12x12x002.model.Event
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseEventRepository(
    private val firestore: FirebaseFirestore
) : EventRepository {
    override fun observeEvents(): Flow<List<Event>> = callbackFlow {
        val listener = firestore.collection(EVENTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(Event::class.java)?.copy(id = document.id)
                    }
                    .orEmpty()

                trySend(events)
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun observeEvent(eventId: String): Flow<Event?> = callbackFlow {
        val listener = firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val event = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
                } else {
                    null
                }

                trySend(event)
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun observeMessages(eventId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents
                    ?.mapNotNull { document -> document.toObject(ChatMessage::class.java) }
                    .orEmpty()

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun addEvent(event: Event) {
        firestore.collection(EVENTS_COLLECTION)
            .add(event)
            .awaitResult()
    }

    override suspend fun updateEvent(event: Event) {
        firestore.collection(EVENTS_COLLECTION)
            .document(event.id)
            .update(
                mapOf(
                    "title" to event.title,
                    "location" to event.location,
                    "date" to event.date,
                    "type" to event.type,
                    "timeInfo" to event.timeInfo,
                    "city" to event.city,
                    "authorEmail" to event.authorEmail,
                    "status" to event.status,
                    "statusReason" to event.statusReason
                )
            )
            .awaitResult()
    }

    override suspend fun cancelEvent(eventId: String, reason: String) {
        firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .set(
                mapOf(
                    "status" to EVENT_STATUS_CANCELLED,
                    "statusReason" to reason
                ),
                SetOptions.merge()
            )
            .awaitResult()
    }

    override suspend fun restoreEvent(eventId: String) {
        firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .set(
                mapOf(
                    "status" to EVENT_STATUS_ACTIVE,
                    "statusReason" to ""
                ),
                SetOptions.merge()
            )
            .awaitResult()
    }

    override suspend fun toggleParticipation(eventId: String, userEmail: String, isJoined: Boolean) {
        firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .update(
                "participantEmails",
                if (isJoined) {
                    FieldValue.arrayRemove(userEmail)
                } else {
                    FieldValue.arrayUnion(userEmail)
                }
            )
            .awaitResult()
    }

    override suspend fun sendMessage(eventId: String, message: ChatMessage) {
        firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .collection(MESSAGES_COLLECTION)
            .add(message)
            .awaitResult()

        firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .update("chatCount", FieldValue.increment(1))
            .awaitResult()
    }

    private companion object {
        const val EVENTS_COLLECTION = "events"
        const val MESSAGES_COLLECTION = "messages"
    }
}
