package com.example.asthmahelper.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asthma_attacks")
data class AsthmaAttackLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val severity: String,       // AttackSeverity.name
    val rescueDoses: Int,
    val triggers: String = "",  // список через запятую
    val location: String? = null,
    val notes: String? = null
)
