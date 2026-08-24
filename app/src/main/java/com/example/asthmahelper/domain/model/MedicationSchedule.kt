package com.example.asthmahelper.domain.model

data class MedicationSchedule(
    val id: Long = 0,
    val medicineId: Long,
    val dose: String,
    val timeOfDay: Long, // milliseconds since midnight
    val daysOfWeek: String, // e.g., "1234567" for all days
    val isActive: Boolean = true
)