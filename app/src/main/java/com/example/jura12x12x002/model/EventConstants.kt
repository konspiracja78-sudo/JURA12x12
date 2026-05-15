package com.example.jura12x12x002.model

const val EVENT_TYPE_ROCKS = "Skały"
const val EVENT_TYPE_PANEL = "Panel"
const val EVENT_STATUS_ACTIVE = "Aktywne"
const val EVENT_STATUS_CANCELLED = "Odwołane"
const val ROCKS_TIME_PREFIX = "Wyjazd: "

fun Event.isRocksType(): Boolean = type == EVENT_TYPE_ROCKS

fun Event.isPanelType(): Boolean = type == EVENT_TYPE_PANEL

fun Event.isCancelledStatus(): Boolean = status == EVENT_STATUS_CANCELLED

fun extractRocksDepartureTime(timeInfo: String): String = timeInfo.removePrefix(ROCKS_TIME_PREFIX).trim()
