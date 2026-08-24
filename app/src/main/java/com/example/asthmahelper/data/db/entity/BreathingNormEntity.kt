package com.example.asthmahelper.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breathing_norms")
data class BreathingNormEntity(
    @PrimaryKey
    val id: Long = 1, // Always one record
    val minNormal: Float,
    val maxNormal: Float,
    val formulaType: Int // 0 - manual, 1 - by age/height, etc.
)