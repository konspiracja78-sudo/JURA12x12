package com.example.jura12x12x002.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.R
import com.example.jura12x12x002.model.EVENT_STATUS_ACTIVE
import com.example.jura12x12x002.model.EVENT_TYPE_PANEL
import com.example.jura12x12x002.model.EVENT_TYPE_ROCKS
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.model.ROCKS_TIME_PREFIX
import com.example.jura12x12x002.model.extractRocksDepartureTime
import com.example.jura12x12x002.model.isPanelType
import com.example.jura12x12x002.model.isRocksType
import java.util.Calendar

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
        title = { Text(stringResource(R.string.dialog_cancel_event_title)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_cancel_event_description))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        if (!isSaving) {
                            reason = it
                        }
                    },
                    label = { Text(stringResource(R.string.dialog_cancel_event_reason_label)) },
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
                Text(
                    if (isSaving) {
                        stringResource(R.string.common_saving)
                    } else {
                        stringResource(R.string.dialog_cancel_event_confirm)
                    }
                )
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
                Text(stringResource(R.string.common_cancel))
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

    val initialTypeIndex = if (initialEvent?.isPanelType() == true) 1 else 0

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var city by remember { mutableStateOf(initialEvent?.city ?: "") }
    var date by remember { mutableStateOf(initialEvent?.date ?: "") }

    var selectedTypeIndex by remember { mutableStateOf(initialTypeIndex) }
    val types = listOf(EVENT_TYPE_ROCKS, EVENT_TYPE_PANEL)

    var departureTime by remember {
        mutableStateOf(
            if (initialEvent?.isRocksType() == true) {
                extractRocksDepartureTime(initialEvent.timeInfo)
            } else {
                ""
            }
        )
    }

    var startTime by remember {
        mutableStateOf(
            if (initialEvent?.isPanelType() == true) {
                initialEvent.timeInfo.split(" - ").firstOrNull()?.trim() ?: ""
            } else {
                ""
            }
        )
    }

    var endTime by remember {
        mutableStateOf(
            if (initialEvent?.isPanelType() == true) {
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
            { _, year, month, day ->
                val normalizedMonth = (month + 1).toString().padStart(2, '0')
                val normalizedDay = day.toString().padStart(2, '0')
                date = "$normalizedDay/$normalizedMonth/$year"
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
            { _, hour, minute ->
                val hh = hour.toString().padStart(2, '0')
                val mm = minute.toString().padStart(2, '0')
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
                    text = stringResource(R.string.event_form_type_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    types.forEachIndexed { index, type ->
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
                            Text(
                                if (type == EVENT_TYPE_ROCKS) {
                                    stringResource(R.string.event_type_rocks)
                                } else {
                                    stringResource(R.string.event_type_panel)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (types[selectedTypeIndex] == EVENT_TYPE_ROCKS) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            if (!isSaving) title = it
                        },
                        label = { Text(stringResource(R.string.event_label_destination)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            if (!isSaving) location = it
                        },
                        label = { Text(stringResource(R.string.event_label_departure_from)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            if (!isSaving) location = it
                        },
                        label = { Text(stringResource(R.string.event_label_training_location)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = {
                            if (!isSaving) city = it
                        },
                        label = { Text(stringResource(R.string.event_label_which_city)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.common_date)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { openDatePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.event_form_pick_date))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (types[selectedTypeIndex] == EVENT_TYPE_ROCKS) {
                    Text(stringResource(R.string.event_form_departure_time_title))
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { openTimePicker { departureTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (departureTime.isEmpty()) {
                                stringResource(R.string.event_form_pick_departure_time)
                            } else {
                                departureTime
                            }
                        )
                    }
                } else {
                    Text(stringResource(R.string.event_form_training_hours_title))
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { openTimePicker { startTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (startTime.isEmpty()) {
                                stringResource(R.string.event_form_pick_start_time)
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
                                stringResource(R.string.event_form_pick_end_time)
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
                        Toast.makeText(
                            context,
                            context.getString(R.string.event_form_error_pick_date),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    if (selectedType == EVENT_TYPE_ROCKS) {
                        if (title.isBlank() || location.isBlank() || departureTime.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.event_form_error_fill_rocks),
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
                                timeInfo = "$ROCKS_TIME_PREFIX${departureTime.trim()}",
                                city = "",
                                participantEmails = initialEvent?.participantEmails ?: emptyList(),
                                chatCount = initialEvent?.chatCount ?: 0L,
                                authorEmail = initialEvent?.authorEmail ?: "",
                                status = initialEvent?.status ?: EVENT_STATUS_ACTIVE,
                                statusReason = initialEvent?.statusReason ?: ""
                            )
                        )
                    } else {
                        if (location.isBlank() || city.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.event_form_error_fill_panel),
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
                                timeInfo = "${startTime.trim()} - ${endTime.trim()}",
                                city = city.trim(),
                                participantEmails = initialEvent?.participantEmails ?: emptyList(),
                                chatCount = initialEvent?.chatCount ?: 0L,
                                authorEmail = initialEvent?.authorEmail ?: "",
                                status = initialEvent?.status ?: EVENT_STATUS_ACTIVE,
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
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
