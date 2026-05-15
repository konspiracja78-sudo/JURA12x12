package com.example.jura12x12x002.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.R
import com.example.jura12x12x002.model.ChatMessage
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.model.extractRocksDepartureTime
import com.example.jura12x12x002.model.isRocksType
import com.example.jura12x12x002.ui.theme.JuraCardBackground
import com.example.jura12x12x002.ui.theme.JuraDanger
import com.example.jura12x12x002.ui.theme.JuraGreen
import com.example.jura12x12x002.ui.theme.JuraLight
import com.example.jura12x12x002.ui.theme.JuraSand
import com.example.jura12x12x002.ui.theme.JuraWarmBrown
import com.example.jura12x12x002.utils.isCancelled

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
    val label = if (cancelled) {
        stringResource(R.string.event_status_chip_cancelled)
    } else {
        stringResource(R.string.event_status_chip_active)
    }

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
    val noData = stringResource(R.string.common_no_data)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (event.isRocksType()) {
                            stringResource(R.string.event_card_title_rocks)
                        } else {
                            stringResource(R.string.event_card_title_panel)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JuraWarmBrown
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(event = event)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (event.isRocksType()) {
                InfoLine(stringResource(R.string.event_label_target), event.title.ifBlank { noData })
                InfoLine(stringResource(R.string.event_label_start_from), event.location.ifBlank { noData })
                InfoLine(stringResource(R.string.common_date), event.date.ifBlank { noData })
                InfoLine(
                    stringResource(R.string.common_time),
                    extractRocksDepartureTime(event.timeInfo).ifBlank { noData }
                )
            } else {
                InfoLine(stringResource(R.string.event_label_place), event.location.ifBlank { noData })
                InfoLine(stringResource(R.string.common_city), event.city.ifBlank { noData })
                InfoLine(stringResource(R.string.common_date), event.date.ifBlank { noData })
                InfoLine(stringResource(R.string.common_hours), event.timeInfo.ifBlank { noData })
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (event.authorEmail.isNotBlank()) {
                    stringResource(R.string.event_label_author, event.authorEmail)
                } else {
                    stringResource(R.string.event_label_author_missing)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (isAuthor) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.event_label_is_your_event),
                    style = MaterialTheme.typography.labelLarge,
                    color = JuraWarmBrown
                )
            }

            if (isCancelled(event) && event.statusReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.event_label_reason, event.statusReason),
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
                    text = stringResource(
                        R.string.event_stats_card,
                        event.chatCount,
                        event.participantEmails.size
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (event.chatCount > 0L || event.participantEmails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.event_interest_hint),
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
                    text = if (message.authorEmail.isBlank()) {
                        stringResource(R.string.common_anonymous)
                    } else {
                        message.authorEmail
                    },
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
