package com.example.asthmahelper.domain.model

data class MedicationLog(
    val id: Long = 0,
    val scheduleId: Long,
    val date: Long, // timestamp for the date
    val taken: Boolean
)