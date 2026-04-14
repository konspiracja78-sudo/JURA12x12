package com.example.jura12x12x002.ui.events

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.model.ChatMessage
import com.example.jura12x12x002.model.EVENT_STATUS_ACTIVE
import com.example.jura12x12x002.model.EVENT_STATUS_CANCELLED
import com.example.jura12x12x002.model.EVENT_TYPE_ROCKS
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.ui.components.ChatBubble
import com.example.jura12x12x002.ui.components.InfoLine
import com.example.jura12x12x002.ui.components.StatusChip
import com.example.jura12x12x002.ui.theme.JuraCardBackground
import com.example.jura12x12x002.ui.theme.JuraLight
import com.example.jura12x12x002.ui.theme.JuraWarmBrown
import com.example.jura12x12x002.utils.isCancelled
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    eventId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserEmail = auth.currentUser?.email ?: ""

    var currentEvent by remember { mutableStateOf<Event?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val chatListState = rememberLazyListState()

    var newMessage by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isEventLoading by remember { mutableStateOf(true) }
    var isEditingEvent by remember { mutableStateOf(false) }
    var isSendingMessage by remember { mutableStateOf(false) }
    var isUpdatingStatus by remember { mutableStateOf(false) }

    DisposableEffect(eventId, db, context) {
        val eventListener = db.collection("events")
            .document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isEventLoading = false
                    Toast.makeText(
                        context,
                        "Błąd wydarzenia: ${error.message ?: "nieznany błąd"}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    isEventLoading = false
                    val loadedEvent = snapshot.toObject(Event::class.java)
                    if (loadedEvent != null) {
                        currentEvent = loadedEvent.copy(id = snapshot.id)
                    }
                } else {
                    isEventLoading = false
                }
            }

        val messagesListener = db.collection("events")
            .document(eventId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        context,
                        "Błąd czatu: ${error.message ?: "nieznany błąd"}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    messages.addAll(
                        snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ChatMessage::class.java)
                        }
                    )
                }
            }

        onDispose {
            eventListener.remove()
            messagesListener.remove()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (isEventLoading) {
        EventDetailsStateScaffold(onBack = onBack) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Ładowanie wydarzenia...")
            }
        }
        return
    }

    val event = currentEvent

    if (event == null) {
        EventDetailsStateScaffold(onBack = onBack) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Nie udało się wczytać wydarzenia")
            }
        }
        return
    }

    val cancelled = isCancelled(event)
    val isJoined = currentUserEmail.isNotBlank() && event.participantEmails.contains(currentUserEmail)
    val isAuthor = event.authorEmail.isNotBlank() && event.authorEmail == currentUserEmail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły wydarzenia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Powrót"
                        )
                    }
                },
                actions = {
                    if (isAuthor && !cancelled) {
                        IconButton(
                            onClick = {
                                if (!isEditingEvent) {
                                    showEditDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edytuj wydarzenie"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = JuraCardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        label = { Text("Napisz wiadomość") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (cancelled) {
                                Toast.makeText(context, "Czat jest zablokowany dla odwołanego wydarzenia", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isSendingMessage) return@Button

                            if (event.id.isBlank()) {
                                Toast.makeText(context, "Brak ID wydarzenia", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (newMessage.isBlank()) {
                                Toast.makeText(context, "Wpisz wiadomość", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSendingMessage = true

                            val message = ChatMessage(
                                text = newMessage.trim(),
                                authorEmail = auth.currentUser?.email ?: "Brak emaila",
                                createdAt = System.currentTimeMillis()
                            )

                            db.collection("events")
                                .document(event.id)
                                .collection("messages")
                                .add(message)
                                .addOnSuccessListener {
                                    db.collection("events")
                                        .document(event.id)
                                        .update("chatCount", FieldValue.increment(1))
                                        .addOnSuccessListener {
                                            isSendingMessage = false
                                            newMessage = ""
                                        }
                                        .addOnFailureListener {
                                            isSendingMessage = false
                                            newMessage = ""
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isSendingMessage = false
                                    Toast.makeText(
                                        context,
                                        "Błąd wysyłki: ${e.message ?: "nieznany błąd"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSendingMessage
                    ) {
                        Text(if (isSendingMessage) "Wysyłanie..." else "Wyślij")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = chatListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JuraCardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = if (event.type == EVENT_TYPE_ROCKS) {
                                    "Wyjazd w skały"
                                } else {
                                    "Trening na panelu"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = JuraWarmBrown
                            )
                            StatusChip(event = event)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (event.type == EVENT_TYPE_ROCKS) {
                            InfoLine("Typ", "Skały")
                            InfoLine("Gdzie lecimy", event.title.ifBlank { "-" })
                            InfoLine("Skąd wyjazd", event.location.ifBlank { "-" })
                            InfoLine("Data", event.date.ifBlank { "-" })
                            InfoLine("Godzina", event.timeInfo.ifBlank { "-" })
                        } else {
                            InfoLine("Typ", "Panel")
                            InfoLine("Gdzie ładujemy", event.location.ifBlank { "-" })
                            InfoLine("Miasto", event.city.ifBlank { "-" })
                            InfoLine("Data", event.date.ifBlank { "-" })
                            InfoLine("Godziny", event.timeInfo.ifBlank { "-" })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (event.authorEmail.isNotBlank()) {
                                "Autor wydarzenia: ${event.authorEmail}"
                            } else {
                                "Autor wydarzenia: brak danych"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (isAuthor) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To Ty jesteś autorem tego wydarzenia",
                                style = MaterialTheme.typography.labelLarge,
                                color = JuraWarmBrown
                            )
                        }

                        if (cancelled && event.statusReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Powód odwołania: ${event.statusReason}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = JuraLight,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("💬 Wiadomości na czacie: ${event.chatCount}")
                                Text("👥 Liczba uczestników: ${event.participantEmails.size}")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (event.participantEmails.isEmpty()) {
                    Text("Lista uczestników: brak zapisanych osób")
                } else {
                    Text(
                        text = "Lista uczestników",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JuraWarmBrown
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    event.participantEmails.forEach { email ->
                        Text("• $email")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (cancelled) {
                                Toast.makeText(context, "Nie można dołączyć do odwołanego wydarzenia", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (currentUserEmail.isBlank()) {
                                Toast.makeText(context, "Brak zalogowanego emaila", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            db.collection("events")
                                .document(event.id)
                                .update(
                                    "participantEmails",
                                    if (isJoined) {
                                        FieldValue.arrayRemove(currentUserEmail)
                                    } else {
                                        FieldValue.arrayUnion(currentUserEmail)
                                    }
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        if (isJoined) "Opuściłeś wydarzenie" else "Dołączyłeś do wydarzenia",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Błąd zapisu uczestnika: ${e.message ?: "nieznany błąd"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isJoined) "Opuść wydarzenie" else "Dołącz do wydarzenia")
                    }

                    if (isAuthor && !cancelled) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (!isEditingEvent) {
                                    showEditDialog = true
                                }
                            },
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text("Edytuj")
                        }
                    }
                }

                if (isAuthor) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (isUpdatingStatus) return@Button

                            if (cancelled) {
                                isUpdatingStatus = true

                                db.collection("events")
                                    .document(event.id)
                                    .set(
                                        mapOf(
                                            "status" to EVENT_STATUS_ACTIVE,
                                            "statusReason" to ""
                                        ),
                                        SetOptions.merge()
                                    )
                                    .addOnSuccessListener {
                                        currentEvent = event.copy(
                                            status = EVENT_STATUS_ACTIVE,
                                            statusReason = ""
                                        )
                                        isUpdatingStatus = false
                                        Toast.makeText(context, "Przywrócono wydarzenie", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isUpdatingStatus = false
                                        Toast.makeText(
                                            context,
                                            "Błąd przywracania: ${e.message ?: "nieznany błąd"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            } else {
                                showCancelDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isUpdatingStatus) {
                                "Zapisywanie..."
                            } else {
                                if (cancelled) "Przywróć wydarzenie" else "Odwołaj wydarzenie"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Czat wydarzenia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = JuraWarmBrown
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (messages.isEmpty()) {
                item {
                    Text("Brak wiadomości")
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                items(
                    items = messages,
                    key = { "${it.authorEmail}_${it.createdAt}_${it.text}" }
                ) { message ->
                    ChatBubble(
                        message = message,
                        isMine = currentUserEmail.isNotBlank() && message.authorEmail == currentUserEmail
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showEditDialog) {
        EventFormDialog(
            dialogTitle = "Edytuj wydarzenie",
            initialEvent = event,
            confirmButtonText = if (isEditingEvent) "Zapisywanie..." else "Zapisz",
            isSaving = isEditingEvent,
            onDismiss = {
                if (!isEditingEvent) {
                    showEditDialog = false
                }
            },
            onSave = { updatedEvent ->
                if (!isAuthor) {
                    Toast.makeText(context, "Tylko autor może edytować wydarzenie", Toast.LENGTH_SHORT).show()
                    return@EventFormDialog
                }

                if (isEditingEvent) return@EventFormDialog

                val updateData = hashMapOf<String, Any>(
                    "title" to updatedEvent.title,
                    "location" to updatedEvent.location,
                    "date" to updatedEvent.date,
                    "type" to updatedEvent.type,
                    "timeInfo" to updatedEvent.timeInfo,
                    "city" to updatedEvent.city,
                    "authorEmail" to event.authorEmail,
                    "status" to event.status,
                    "statusReason" to event.statusReason
                )

                isEditingEvent = true

                db.collection("events")
                    .document(event.id)
                    .update(updateData)
                    .addOnSuccessListener {
                        isEditingEvent = false
                        showEditDialog = false
                        Toast.makeText(context, "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        isEditingEvent = false
                        Toast.makeText(
                            context,
                            "Błąd edycji: ${e.message ?: "nieznany błąd"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        )
    }

    if (showCancelDialog) {
        CancelEventDialog(
            onDismiss = {
                if (!isUpdatingStatus) {
                    showCancelDialog = false
                }
            },
            onConfirm = { reason ->
                if (!isAuthor) {
                    Toast.makeText(context, "Tylko autor może odwołać wydarzenie", Toast.LENGTH_SHORT).show()
                    return@CancelEventDialog
                }

                if (isUpdatingStatus) return@CancelEventDialog

                isUpdatingStatus = true

                db.collection("events")
                    .document(event.id)
                    .set(
                        mapOf(
                            "status" to EVENT_STATUS_CANCELLED,
                            "statusReason" to reason.trim()
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        currentEvent = event.copy(
                            status = EVENT_STATUS_CANCELLED,
                            statusReason = reason.trim()
                        )
                        isUpdatingStatus = false
                        showCancelDialog = false
                        Toast.makeText(context, "Wydarzenie zostało odwołane", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        isUpdatingStatus = false
                        Toast.makeText(
                            context,
                            "Błąd odwołania: ${e.message ?: "nieznany błąd"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            isSaving = isUpdatingStatus
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailsStateScaffold(
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły wydarzenia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Powrót"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopStart
        ) {
            content()
        }
    }
}
