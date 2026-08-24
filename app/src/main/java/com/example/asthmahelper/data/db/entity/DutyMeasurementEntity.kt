package com.example.asthmahelper.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "duty_measurements")
data class DutyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Float,
    val timestamp: Long,
    val note: String? = null
)