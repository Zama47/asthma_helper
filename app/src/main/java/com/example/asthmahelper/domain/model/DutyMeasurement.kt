package com.example.asthmahelper.domain.model

data class DutyMeasurement(
    val id: Long = 0,
    val value: Float,
    val timestamp: Long,
    val note: String? = null
)