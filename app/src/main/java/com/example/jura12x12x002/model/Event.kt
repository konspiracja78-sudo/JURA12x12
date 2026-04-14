package com.example.jura12x12x002.model

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
    val status: String = EVENT_STATUS_ACTIVE,
    val statusReason: String = ""
)
