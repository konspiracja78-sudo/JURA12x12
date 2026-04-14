package com.example.jura12x12x002.utils

import com.example.jura12x12x002.model.EVENT_STATUS_CANCELLED
import com.example.jura12x12x002.model.EVENT_TYPE_ROCKS
import com.example.jura12x12x002.model.Event
import com.example.jura12x12x002.model.ROCKS_TIME_PREFIX
import java.text.SimpleDateFormat
import java.util.Locale

fun parseDateTime(event: Event): Long {
    return try {
        val timePart = if (event.type == EVENT_TYPE_ROCKS) {
            event.timeInfo.removePrefix(ROCKS_TIME_PREFIX).trim()
        } else {
            event.timeInfo.split(" - ").firstOrNull()?.trim() ?: "00:00"
        }

        val full = "${event.date} $timePart"
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pl", "PL"))
        format.isLenient = false
        format.parse(full)?.time ?: Long.MAX_VALUE
    } catch (_: Exception) {
        Long.MAX_VALUE
    }
}

fun sanitizeEmail(email: String): String = email.trim()

fun isCancelled(event: Event): Boolean = event.status == EVENT_STATUS_CANCELLED
