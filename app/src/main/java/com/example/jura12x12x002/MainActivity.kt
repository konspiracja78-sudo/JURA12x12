package com.example.jura12x12x002

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val JuraSand = Color(0xFFD8C3A5)
private val JuraRock = Color(0xFFB7A28A)
private val JuraLight = Color(0xFFF4ECE1)
private val JuraGreen = Color(0xFFDDE8D5)
private val JuraWarmBrown = Color(0xFF7A5C3E)
private val JuraDanger = Color(0xFFFFE5E5)

data class Event(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val date: String = "",
    val type: String = "",
    val timeInfo: String = "",
    val city: String = "",
    val participantEmails: List<String> = emptyList(),
    val chatCount: Long = 0L,
    val authorEmail: String = "",
    val status: String = "Aktywne",
    val statusReason: String = ""
)

data class ChatMessage(
    val text: String = "",
    val authorEmail: String = "",
    val createdAt: Long = 0L
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuraApp()
        }
    }
}

@Composable
fun JuraApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    if (isLoggedIn) {
        MainScreen(
            onLogout = {
                auth.signOut()
                isLoggedIn = false
            }
        )
    } else {
        AuthScreen(
            onAuthSuccess = {
                isLoggedIn = true
            }
        )
    }
}

fun parseDateTime(event: Event): Long {
    return try {
        val timePart = if (event.type == "Skały") {
            event.timeInfo.removePrefix("Wyjazd: ").trim()
        } else {
            event.timeInfo.split(" - ").firstOrNull()?.trim() ?: "00:00"
        }

        val full = "${event.date} $timePart"
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pl", "PL"))
        format.isLenient = false
        format.parse(full)?.time ?: Long.MAX_VALUE
    } catch (e: Exception) {
        Long.MAX_VALUE
    }
}

fun sanitizeEmail(email: String): String {
    return email.trim()
}

fun isCancelled(event: Event): Boolean {
    return event.status == "Odwołane"
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) }

    if (isRegisterMode) {
        RegisterScreen(
            onRegisterSuccess = onAuthSuccess,
            onGoToLogin = { isRegisterMode = false }
        )
    } else {
        LoginScreen(
            onLoginSuccess = onAuthSuccess,
            onGoToRegister = { isRegisterMode = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val allEvents = remember { mutableStateListOf<Event>() }

    var listener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var isEventsLoading by remember { mutableStateOf(true) }
    var isAddingEvent by remember { mutableStateOf(false) }
    var selectedFilterIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf("Wszystkie", "Aktywne", "Odwołane")

    DisposableEffect(Unit) {
        listener = db.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isEventsLoading = false
                    Toast.makeText(
                        context,
                        "Błąd pobierania wydarzeń: ${error.message ?: "nieznany błąd"}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    isEventsLoading = false
                    allEvents.clear()

                    for (doc in snapshot.documents) {
                        val event = doc.toObject(Event::class.java)
                        if (event != null) {
                            allEvents.add(event.copy(id = doc.id))
                        }
                    }
                }
            }

        onDispose {
            listener?.remove()
        }
    }

    if (selectedEventId != null) {
        EventDetailsScreen(
            eventId = selectedEventId!!,
            onBack = { selectedEventId = null }
        )
        return
    }

    val sortedEvents = allEvents.sortedBy { parseDateTime(it) }

    val filteredByStatus = when (filterOptions[selectedFilterIndex]) {
        "Aktywne" -> sortedEvents.filter { !isCancelled(it) }
        "Odwołane" -> sortedEvents.filter { isCancelled(it) }
        else -> sortedEvents
    }

    val filteredEvents = if (searchQuery.isBlank()) {
        filteredByStatus
    } else {
        val query = searchQuery.trim().lowercase()
        filteredByStatus.filter { event ->
            event.title.lowercase().contains(query) ||
                    event.location.lowercase().contains(query) ||
                    event.city.lowercase().contains(query)
        }
    }

    val skalyEvents = filteredEvents.filter { it.type == "Skały" }
    val panelEvents = filteredEvents.filter { it.type == "Panel" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JURA12x12") },
                actions = {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Wyloguj")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isAddingEvent) {
                        showAddDialog = true
                    }
                },
                containerColor = JuraRock,
                contentColor = Color.White
            ) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Filtr wydarzeń",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = JuraWarmBrown
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                filterOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = filterOptions.size
                        ),
                        onClick = {
                            selectedFilterIndex = index
                        },
                        selected = index == selectedFilterIndex
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JuraLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj: cel / miejsce / miasto") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Szukaj"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEventsLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Ładowanie wydarzeń...")
            } else if (allEvents.isEmpty()) {
                Text("Brak wydarzeń w bazie")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Dodaj pierwsze wydarzenie przyciskiem +")
            } else if (filteredEvents.isEmpty()) {
                Text("Brak wydarzeń dla wybranego filtra lub wyszukiwania")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SectionHeader(title = "🧗 Skały")
                    }

                    if (skalyEvents.isEmpty()) {
                        item {
                            Text("Brak wydarzeń w sekcji Skały")
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    } else {
                        items(
                            items = skalyEvents,
                            key = { it.id }
                        ) { event ->
                            EventCard(
                                event = event,
                                currentUserEmail = auth.currentUser?.email ?: "",
                                onClick = { selectedEventId = event.id }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = "🏠 Panel")
                    }

                    if (panelEvents.isEmpty()) {
                        item {
                            Text("Brak wydarzeń w sekcji Panel")
                        }
                    } else {
                        items(
                            items = panelEvents,
                            key = { it.id }
                        ) { event ->
                            EventCard(
                                event = event,
                                currentUserEmail = auth.currentUser?.email ?: "",
                                onClick = { selectedEventId = event.id }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EventFormDialog(
            dialogTitle = "Nowe wydarzenie",
            initialEvent = null,
            confirmButtonText = if (isAddingEvent) "Dodawanie..." else "Dodaj",
            isSaving = isAddingEvent,
            onDismiss = {
                if (!isAddingEvent) {
                    showAddDialog = false
                }
            },
            onSave = { event ->
                if (isAddingEvent) return@EventFormDialog

                val currentUserEmail = auth.currentUser?.email ?: ""
                val eventToSave = event.copy(
                    id = "",
                    authorEmail = currentUserEmail,
                    status = "Aktywne",
                    statusReason = ""
                )

                isAddingEvent = true

                db.collection("events")
                    .add(eventToSave)
                    .addOnSuccessListener {
                        isAddingEvent = false
                        showAddDialog = false
                        Toast.makeText(context, "Dodano wydarzenie", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        isAddingEvent = false
                        Toast.makeText(
                            context,
                            "Błąd dodawania wydarzenia: ${e.message ?: "nieznany błąd"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = JuraLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = JuraWarmBrown
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = JuraSand)
        }
    }
}

@Composable
fun StatusChip(event: Event) {
    val cancelled = isCancelled(event)
    val containerColor = if (cancelled) JuraDanger else JuraGreen
    val label = if (cancelled) "ODWOŁANE" else "AKTYWNE"

    FilterChip(
        selected = true,
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = label,
                fontWeight = FontWeight.Bold
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            disabledContainerColor = containerColor,
            disabledLabelColor = Color.Black
        )
    )
}

@Composable
fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = JuraWarmBrown
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EventCard(
    event: Event,
    currentUserEmail: String,
    onClick: () -> Unit
) {
    val isAuthor = event.authorEmail.isNotBlank() && event.authorEmail == currentUserEmail

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFCF8)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (event.type == "Skały") "Wyjazd w skały" else "Trening na panelu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JuraWarmBrown
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(event = event)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (event.type == "Skały") {
                InfoLine("Cel", event.title.ifBlank { "-" })
                InfoLine("Start / skąd", event.location.ifBlank { "-" })
                InfoLine("Data", event.date.ifBlank { "-" })
                InfoLine("Godzina", event.timeInfo.ifBlank { "-" })
            } else {
                InfoLine("Miejsce", event.location.ifBlank { "-" })
                InfoLine("Miasto", event.city.ifBlank { "-" })
                InfoLine("Data", event.date.ifBlank { "-" })
                InfoLine("Godziny", event.timeInfo.ifBlank { "-" })
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (event.authorEmail.isNotBlank()) {
                Text(
                    text = "Autor: ${event.authorEmail}",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Autor: brak danych",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isAuthor) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To Twoje wydarzenie",
                    style = MaterialTheme.typography.labelLarge,
                    color = JuraWarmBrown
                )
            }

            if (isCancelled(event) && event.statusReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Powód: ${event.statusReason}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = JuraLight,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = "💬 Czat: ${event.chatCount}   |   👥 Uczestnicy: ${event.participantEmails.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (event.chatCount > 0L || event.participantEmails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To wydarzenie już kogoś interesuje",
                    style = MaterialTheme.typography.labelMedium,
                    color = JuraWarmBrown
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) {
                    JuraSand
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = if (message.authorEmail.isBlank()) "Anonim" else message.authorEmail,
                    style = MaterialTheme.typography.labelMedium,
                    color = JuraWarmBrown,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

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

    var eventListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var messagesListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    var newMessage by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isEventLoading by remember { mutableStateOf(true) }
    var isEditingEvent by remember { mutableStateOf(false) }
    var isSendingMessage by remember { mutableStateOf(false) }
    var isUpdatingStatus by remember { mutableStateOf(false) }

    DisposableEffect(eventId) {
        eventListener = db.collection("events")
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

        messagesListener = db.collection("events")
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
                    val loadedMessages = snapshot.documents
                        .mapNotNull { doc -> doc.toObject(ChatMessage::class.java) }

                    messages.addAll(loadedMessages)
                }
            }

        onDispose {
            eventListener?.remove()
            messagesListener?.remove()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (isEventLoading) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8))
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8))
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
                                text = if (event.type == "Skały") "Wyjazd w skały" else "Trening na panelu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = JuraWarmBrown
                            )
                            StatusChip(event = event)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (event.type == "Skały") {
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
                                            "status" to "Aktywne",
                                            "statusReason" to ""
                                        ),
                                        SetOptions.merge()
                                    )
                                    .addOnSuccessListener {
                                        currentEvent = event.copy(
                                            status = "Aktywne",
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
                            "status" to "Odwołane",
                            "statusReason" to reason.trim()
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        currentEvent = event.copy(
                            status = "Odwołane",
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

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isResettingPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Logowanie",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val loginEmail = sanitizeEmail(email)

                if (loginEmail.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Uzupełnij email i hasło", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.signOut()

                auth.signInWithEmailAndPassword(loginEmail, password)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Zalogowano", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Błąd logowania. Sprawdź dokładnie ten sam email i nowe hasło z resetu. Szczegóły: ${e.message ?: "spróbuj ponownie"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zaloguj")
        }

        Button(
            onClick = {
                if (isResettingPassword) return@Button

                val resetEmail = sanitizeEmail(email)

                if (resetEmail.isBlank()) {
                    Toast.makeText(context, "Najpierw wpisz email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isResettingPassword = true

                auth.sendPasswordResetEmail(resetEmail)
                    .addOnSuccessListener {
                        isResettingPassword = false
                        Toast.makeText(
                            context,
                            "Wysłano mail do resetu hasła",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        isResettingPassword = false
                        Toast.makeText(
                            context,
                            "Błąd resetu hasła: ${e.message ?: "spróbuj ponownie"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isResettingPassword) "Wysyłanie..." else "Resetuj hasło")
        }

        TextButton(
            onClick = onGoToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nie masz konta? Przejdź do rejestracji")
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Rejestracja",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = repeatPassword,
            onValueChange = { repeatPassword = it },
            label = { Text("Powtórz hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val registerEmail = sanitizeEmail(email)

                if (registerEmail.isBlank() || password.isBlank() || repeatPassword.isBlank()) {
                    Toast.makeText(context, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password != repeatPassword) {
                    Toast.makeText(context, "Hasła nie są takie same", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password.length < 6) {
                    Toast.makeText(context, "Hasło musi mieć co najmniej 6 znaków", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.createUserWithEmailAndPassword(registerEmail, password)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Konto utworzone", Toast.LENGTH_SHORT).show()
                        onRegisterSuccess()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Błąd rejestracji: ${e.message ?: "spróbuj ponownie"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zarejestruj się")
        }

        TextButton(
            onClick = onGoToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Masz już konto? Wróć do logowania")
        }
    }
}

@Composable
fun CancelEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isSaving: Boolean
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = { Text("Odwołaj wydarzenie") },
        text = {
            Column {
                Text("Możesz wpisać krótki powód odwołania.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        if (!isSaving) {
                            reason = it
                        }
                    },
                    label = { Text("Powód odwołania") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving) {
                        onConfirm(reason)
                    }
                }
            ) {
                Text(if (isSaving) "Zapisywanie..." else "Odwołaj")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    if (!isSaving) {
                        onDismiss()
                    }
                }
            ) {
                Text("Anuluj")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDialog(
    dialogTitle: String,
    initialEvent: Event?,
    confirmButtonText: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val initialTypeIndex = if (initialEvent?.type == "Panel") 1 else 0

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var city by remember { mutableStateOf(initialEvent?.city ?: "") }
    var date by remember { mutableStateOf(initialEvent?.date ?: "") }

    var selectedTypeIndex by remember { mutableStateOf(initialTypeIndex) }
    val types = listOf("Skały", "Panel")

    var departureTime by remember {
        mutableStateOf(
            if (initialEvent?.type == "Skały") {
                initialEvent.timeInfo.removePrefix("Wyjazd: ").trim()
            } else {
                ""
            }
        )
    }

    var startTime by remember {
        mutableStateOf(
            if (initialEvent?.type == "Panel") {
                initialEvent.timeInfo.split(" - ").firstOrNull()?.trim() ?: ""
            } else {
                ""
            }
        )
    }

    var endTime by remember {
        mutableStateOf(
            if (initialEvent?.type == "Panel") {
                initialEvent.timeInfo.split(" - ").getOrNull(1)?.trim() ?: ""
            } else {
                ""
            }
        )
    }

    fun openDatePicker() {
        if (isSaving) return

        DatePickerDialog(
            context,
            { _, y, m, d ->
                val month = (m + 1).toString().padStart(2, '0')
                val day = d.toString().padStart(2, '0')
                date = "$day/$month/$y"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker(onTime: (String) -> Unit) {
        if (isSaving) return

        TimePickerDialog(
            context,
            { _, h, min ->
                val hh = h.toString().padStart(2, '0')
                val mm = min.toString().padStart(2, '0')
                onTime("$hh:$mm")
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = { Text(dialogTitle) },
        text = {
            Column {
                Text(
                    text = "Typ wydarzenia",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    types.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = types.size
                            ),
                            onClick = {
                                if (isSaving) return@SegmentedButton

                                selectedTypeIndex = index
                                title = ""
                                location = ""
                                city = ""
                                departureTime = ""
                                startTime = ""
                                endTime = ""
                            },
                            selected = index == selectedTypeIndex
                        ) {
                            Text(label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (types[selectedTypeIndex] == "Skały") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            if (!isSaving) title = it
                        },
                        label = { Text("Gdzie lecimy") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            if (!isSaving) location = it
                        },
                        label = { Text("Skąd wyjazd") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            if (!isSaving) location = it
                        },
                        label = { Text("Gdzie ładujemy") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = {
                            if (!isSaving) city = it
                        },
                        label = { Text("Jakie miasto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Data") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { openDatePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wybierz datę")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (types[selectedTypeIndex] == "Skały") {
                    Text("Godzina wyjazdu")
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { openTimePicker { departureTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (departureTime.isEmpty()) {
                                "Wybierz godzinę wyjazdu"
                            } else {
                                departureTime
                            }
                        )
                    }
                } else {
                    Text("Godziny treningu")
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { openTimePicker { startTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (startTime.isEmpty()) {
                                "Wybierz godzinę startu"
                            } else {
                                startTime
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { openTimePicker { endTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (endTime.isEmpty()) {
                                "Wybierz godzinę końca"
                            } else {
                                endTime
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSaving) return@Button

                    val selectedType = types[selectedTypeIndex]

                    if (date.isBlank()) {
                        Toast.makeText(context, "Wybierz datę", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedType == "Skały") {
                        if (title.isBlank() || location.isBlank() || departureTime.isBlank()) {
                            Toast.makeText(
                                context,
                                "Uzupełnij gdzie lecimy, skąd wyjazd i godzinę wyjazdu",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        onSave(
                            Event(
                                id = initialEvent?.id ?: "",
                                title = title.trim(),
                                location = location.trim(),
                                date = date,
                                type = selectedType,
                                timeInfo = "Wyjazd: $departureTime",
                                city = "",
                                participantEmails = initialEvent?.participantEmails ?: emptyList(),
                                chatCount = initialEvent?.chatCount ?: 0L,
                                authorEmail = initialEvent?.authorEmail ?: "",
                                status = initialEvent?.status ?: "Aktywne",
                                statusReason = initialEvent?.statusReason ?: ""
                            )
                        )
                    } else {
                        if (location.isBlank() || city.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                            Toast.makeText(
                                context,
                                "Uzupełnij gdzie ładujemy, miasto i godziny treningu",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        onSave(
                            Event(
                                id = initialEvent?.id ?: "",
                                title = "",
                                location = location.trim(),
                                date = date,
                                type = selectedType,
                                timeInfo = "$startTime - $endTime",
                                city = city.trim(),
                                participantEmails = initialEvent?.participantEmails ?: emptyList(),
                                chatCount = initialEvent?.chatCount ?: 0L,
                                authorEmail = initialEvent?.authorEmail ?: "",
                                status = initialEvent?.status ?: "Aktywne",
                                statusReason = initialEvent?.statusReason ?: ""
                            )
                        )
                    }
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    if (!isSaving) {
                        onDismiss()
                    }
                }
            ) {
                Text("Anuluj")
            }
        }
    )
}