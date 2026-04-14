package com.example.jura12x12x002.ui.events

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.model.EVENT_STATUS_ACTIVE
import com.example.jura12x12x002.model.EVENT_STATUS_CANCELLED
import com.example.jura12x12x002.model.EVENT_TYPE_PANEL
import com.example.jura12x12x002.model.EVENT_TYPE_ROCKS
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.ui.components.EventCard
import com.example.jura12x12x002.ui.components.SectionHeader
import com.example.jura12x12x002.ui.theme.JuraLight
import com.example.jura12x12x002.ui.theme.JuraRock
import com.example.jura12x12x002.ui.theme.JuraWarmBrown
import com.example.jura12x12x002.utils.isCancelled
import com.example.jura12x12x002.utils.parseDateTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val allEvents = remember { mutableStateListOf<Event>() }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var isEventsLoading by remember { mutableStateOf(true) }
    var isAddingEvent by remember { mutableStateOf(false) }
    var selectedFilterIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf("Wszystkie", EVENT_STATUS_ACTIVE, EVENT_STATUS_CANCELLED)

    DisposableEffect(db, context) {
        val listener = db.collection("events")
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

                    snapshot.documents
                        .mapNotNull { doc -> doc.toObject(Event::class.java)?.copy(id = doc.id) }
                        .forEach(allEvents::add)
                }
            }

        onDispose {
            listener.remove()
        }
    }

    if (selectedEventId != null) {
        EventDetailsScreen(
            eventId = selectedEventId!!,
            onBack = { selectedEventId = null }
        )
        return
    }

    val sortedEvents = allEvents.sortedBy(::parseDateTime)
    val filteredByStatus = when (filterOptions[selectedFilterIndex]) {
        EVENT_STATUS_ACTIVE -> sortedEvents.filter { !isCancelled(it) }
        EVENT_STATUS_CANCELLED -> sortedEvents.filter(::isCancelled)
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

    val skalyEvents = filteredEvents.filter { it.type == EVENT_TYPE_ROCKS }
    val panelEvents = filteredEvents.filter { it.type == EVENT_TYPE_PANEL }

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
                        SectionHeader(title = "🟠 Panel")
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
                    status = EVENT_STATUS_ACTIVE,
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
