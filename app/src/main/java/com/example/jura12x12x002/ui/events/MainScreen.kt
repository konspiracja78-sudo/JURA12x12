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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jura12x12x002.R
import com.example.jura12x12x002.di.LocalAppContainer
import com.example.jura12x12x002.di.mainViewModelFactory
import com.example.jura12x12x002.model.isPanelType
import com.example.jura12x12x002.model.isRocksType
import com.example.jura12x12x002.ui.asString
import com.example.jura12x12x002.ui.components.EventCard
import com.example.jura12x12x002.ui.components.SectionHeader
import com.example.jura12x12x002.ui.theme.JuraLight
import com.example.jura12x12x002.ui.theme.JuraRock
import com.example.jura12x12x002.ui.theme.JuraWarmBrown
import com.example.jura12x12x002.utils.isCancelled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenEvent: (String) -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val viewModel: MainViewModel = viewModel(
        factory = mainViewModelFactory(container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    val filterOptions = listOf(
        stringResource(R.string.event_filter_all),
        stringResource(R.string.event_status_active),
        stringResource(R.string.event_status_cancelled)
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message.asString(context), Toast.LENGTH_SHORT).show()
                }
                MainUiEffect.EventSaved -> {
                    showAddDialog = false
                }
            }
        }
    }

    val filteredByStatus = when (uiState.selectedFilterIndex) {
        1 -> uiState.allEvents.filter { !isCancelled(it) }
        2 -> uiState.allEvents.filter(::isCancelled)
        else -> uiState.allEvents
    }

    val filteredEvents = if (uiState.searchQuery.isBlank()) {
        filteredByStatus
    } else {
        val query = uiState.searchQuery.trim().lowercase()
        filteredByStatus.filter { event ->
            event.title.lowercase().contains(query) ||
                event.location.lowercase().contains(query) ||
                event.city.lowercase().contains(query)
        }
    }

    val rocksEvents = filteredEvents.filter { it.isRocksType() }
    val panelEvents = filteredEvents.filter { it.isPanelType() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Button(
                        onClick = viewModel::logout,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.event_logout))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!uiState.isAddingEvent) {
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
                text = stringResource(R.string.event_filter_title),
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
                            viewModel.onFilterSelected(index)
                        },
                        selected = index == uiState.selectedFilterIndex
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
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = { Text(stringResource(R.string.event_search_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.common_search)
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

            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.event_loading_list))
            } else if (uiState.allEvents.isEmpty()) {
                Text(stringResource(R.string.event_empty_database))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.event_empty_database_cta))
            } else if (filteredEvents.isEmpty()) {
                Text(stringResource(R.string.event_empty_filtered))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SectionHeader(title = stringResource(R.string.event_section_rocks))
                    }

                    if (rocksEvents.isEmpty()) {
                        item {
                            Text(stringResource(R.string.event_empty_rocks))
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    } else {
                        items(
                            items = rocksEvents,
                            key = { it.id }
                        ) { event ->
                            EventCard(
                                event = event,
                                currentUserEmail = uiState.currentUserEmail,
                                onClick = { onOpenEvent(event.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = stringResource(R.string.event_section_panel))
                    }

                    if (panelEvents.isEmpty()) {
                        item {
                            Text(stringResource(R.string.event_empty_panel))
                        }
                    } else {
                        items(
                            items = panelEvents,
                            key = { it.id }
                        ) { event ->
                            EventCard(
                                event = event,
                                currentUserEmail = uiState.currentUserEmail,
                                onClick = { onOpenEvent(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EventFormDialog(
            dialogTitle = stringResource(R.string.event_new_title),
            initialEvent = null,
            confirmButtonText = if (uiState.isAddingEvent) {
                stringResource(R.string.event_adding_button)
            } else {
                stringResource(R.string.event_add_button)
            },
            isSaving = uiState.isAddingEvent,
            onDismiss = {
                if (!uiState.isAddingEvent) {
                    showAddDialog = false
                }
            },
            onSave = viewModel::addEvent
        )
    }
}
