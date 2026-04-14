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
import androidx.compose.ui.unit.dp
import com.example.jura12x12x002.model.EVENT_STATUS_ACTIVE
import com.example.jura12x12x002.model.EVENT_TYPE_PANEL
import com.example.jura12x12x002.model.EVENT_TYPE_ROCKS
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.model.ROCKS_TIME_PREFIX
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

    val initialTypeIndex = if (initialEvent?.type == EVENT_TYPE_PANEL) 1 else 0

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var city by remember { mutableStateOf(initialEvent?.city ?: "") }
    var date by remember { mutableStateOf(initialEvent?.date ?: "") }

    var selectedTypeIndex by remember { mutableStateOf(initialTypeIndex) }
    val types = listOf(EVENT_TYPE_ROCKS, EVENT_TYPE_PANEL)

    var departureTime by remember {
        mutableStateOf(
            if (initialEvent?.type == EVENT_TYPE_ROCKS) {
                initialEvent.timeInfo.removePrefix(ROCKS_TIME_PREFIX).trim()
            } else {
                ""
            }
        )
    }

    var startTime by remember {
        mutableStateOf(
            if (initialEvent?.type == EVENT_TYPE_PANEL) {
                initialEvent.timeInfo.split(" - ").firstOrNull()?.trim() ?: ""
            } else {
                ""
            }
        )
    }

    var endTime by remember {
        mutableStateOf(
            if (initialEvent?.type == EVENT_TYPE_PANEL) {
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

                if (types[selectedTypeIndex] == EVENT_TYPE_ROCKS) {
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

                if (types[selectedTypeIndex] == EVENT_TYPE_ROCKS) {
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

                    if (selectedType == EVENT_TYPE_ROCKS) {
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
                                timeInfo = "$ROCKS_TIME_PREFIX$departureTime",
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
                Text("Anuluj")
            }
        }
    )
}
