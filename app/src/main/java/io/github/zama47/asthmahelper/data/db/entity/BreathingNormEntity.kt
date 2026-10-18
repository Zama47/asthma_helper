package io.github.zama47.asthmahelper.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breathing_norms")
data class BreathingNormEntity(
    // Always one record
    @PrimaryKey
    val id: Long = 1,
    val minNormal: Float,
    val maxNormal: Float,
    // 0 - manual, 1 - by age/height, etc.
    val formulaType: Int
)
