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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jura12x12x002.R
import com.example.jura12x12x002.di.LocalAppContainer
import com.example.jura12x12x002.di.eventDetailsViewModelFactory
import com.example.jura12x12x002.model.extractRocksDepartureTime
import com.example.jura12x12x002.model.isRocksType
import com.example.jura12x12x002.ui.asString
import com.example.jura12x12x002.ui.components.ChatBubble
import com.example.jura12x12x002.ui.components.InfoLine
import com.example.jura12x12x002.ui.components.StatusChip
import com.example.jura12x12x002.ui.theme.JuraCardBackground
import com.example.jura12x12x002.ui.theme.JuraLight
import com.example.jura12x12x002.ui.theme.JuraWarmBrown
import com.example.jura12x12x002.utils.isCancelled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    eventId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val viewModel: EventDetailsViewModel = viewModel(
        factory = eventDetailsViewModelFactory(container, eventId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatListState = rememberLazyListState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EventDetailsUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message.asString(context), Toast.LENGTH_SHORT).show()
                }
                EventDetailsUiEffect.CloseEditDialog -> {
                    showEditDialog = false
                }
                EventDetailsUiEffect.CloseCancelDialog -> {
                    showCancelDialog = false
                }
            }
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    if (uiState.isEventLoading) {
        EventDetailsStateScaffold(onBack = onBack) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.event_details_loading))
            }
        }
        return
    }

    val event = uiState.currentEvent

    if (event == null) {
        EventDetailsStateScaffold(onBack = onBack) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.event_details_load_error))
            }
        }
        return
    }

    val cancelled = isCancelled(event)
    val isJoined = uiState.currentUserEmail.isNotBlank() && event.participantEmails.contains(uiState.currentUserEmail)
    val isAuthor = event.authorEmail.isNotBlank() && event.authorEmail == uiState.currentUserEmail
    val noData = stringResource(R.string.common_no_data)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    if (isAuthor && !cancelled) {
                        IconButton(
                            onClick = {
                                if (!uiState.isEditingEvent) {
                                    showEditDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.event_details_edit_cd)
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
                        value = uiState.newMessage,
                        onValueChange = viewModel::onMessageChange,
                        label = { Text(stringResource(R.string.event_details_message_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = viewModel::sendMessage,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSendingMessage
                    ) {
                        Text(
                            if (uiState.isSendingMessage) {
                                stringResource(R.string.event_details_sending_button)
                            } else {
                                stringResource(R.string.event_details_send_button)
                            }
                        )
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
                                text = if (event.isRocksType()) {
                                    stringResource(R.string.event_card_title_rocks)
                                } else {
                                    stringResource(R.string.event_card_title_panel)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = JuraWarmBrown
                            )
                            StatusChip(event = event)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (event.isRocksType()) {
                            InfoLine(stringResource(R.string.common_type), stringResource(R.string.event_type_rocks))
                            InfoLine(stringResource(R.string.event_label_destination), event.title.ifBlank { noData })
                            InfoLine(stringResource(R.string.event_label_departure_from), event.location.ifBlank { noData })
                            InfoLine(stringResource(R.string.common_date), event.date.ifBlank { noData })
                            InfoLine(
                                stringResource(R.string.common_time),
                                extractRocksDepartureTime(event.timeInfo).ifBlank { noData }
                            )
                        } else {
                            InfoLine(stringResource(R.string.common_type), stringResource(R.string.event_type_panel))
                            InfoLine(stringResource(R.string.event_label_training_location), event.location.ifBlank { noData })
                            InfoLine(stringResource(R.string.common_city), event.city.ifBlank { noData })
                            InfoLine(stringResource(R.string.common_date), event.date.ifBlank { noData })
                            InfoLine(stringResource(R.string.common_hours), event.timeInfo.ifBlank { noData })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (event.authorEmail.isNotBlank()) {
                                stringResource(R.string.event_label_event_author, event.authorEmail)
                            } else {
                                stringResource(R.string.event_label_event_author_missing)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (isAuthor) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.event_label_you_are_author),
                                style = MaterialTheme.typography.labelLarge,
                                color = JuraWarmBrown
                            )
                        }

                        if (cancelled && event.statusReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.event_label_cancel_reason, event.statusReason),
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
                                Text(stringResource(R.string.event_chat_count, event.chatCount))
                                Text(stringResource(R.string.event_participants_count, event.participantEmails.size))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (event.participantEmails.isEmpty()) {
                    Text(stringResource(R.string.event_participants_empty))
                } else {
                    Text(
                        text = stringResource(R.string.event_participants_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JuraWarmBrown
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    event.participantEmails.forEach { email ->
                        Text(stringResource(R.string.event_participant_item, email))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = viewModel::toggleParticipation,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isJoined) {
                                stringResource(R.string.event_leave_button)
                            } else {
                                stringResource(R.string.event_join_button)
                            }
                        )
                    }

                    if (isAuthor && !cancelled) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (!uiState.isEditingEvent) {
                                    showEditDialog = true
                                }
                            },
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(stringResource(R.string.common_edit))
                        }
                    }
                }

                if (isAuthor) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (uiState.isUpdatingStatus) return@Button
                            if (cancelled) {
                                viewModel.restoreEvent()
                            } else {
                                showCancelDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isUpdatingStatus) {
                                stringResource(R.string.common_saving)
                            } else {
                                if (cancelled) {
                                    stringResource(R.string.event_restore_button)
                                } else {
                                    stringResource(R.string.event_cancel_button)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.event_chat_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = JuraWarmBrown
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.messages.isEmpty()) {
                item {
                    Text(stringResource(R.string.event_no_messages))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                items(
                    items = uiState.messages,
                    key = { "${it.authorEmail}_${it.createdAt}_${it.text}" }
                ) { message ->
                    ChatBubble(
                        message = message,
                        isMine = uiState.currentUserEmail.isNotBlank() && message.authorEmail == uiState.currentUserEmail
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
            dialogTitle = stringResource(R.string.event_edit_title),
            initialEvent = event,
            confirmButtonText = if (uiState.isEditingEvent) {
                stringResource(R.string.common_saving)
            } else {
                stringResource(R.string.common_save)
            },
            isSaving = uiState.isEditingEvent,
            onDismiss = {
                if (!uiState.isEditingEvent) {
                    showEditDialog = false
                }
            },
            onSave = viewModel::updateEvent
        )
    }

    if (showCancelDialog) {
        CancelEventDialog(
            onDismiss = {
                if (!uiState.isUpdatingStatus) {
                    showCancelDialog = false
                }
            },
            onConfirm = viewModel::cancelEvent,
            isSaving = uiState.isUpdatingStatus
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
                title = { Text(stringResource(R.string.event_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
