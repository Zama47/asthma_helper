package io.github.zama47.asthmahelper.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Результат прохождения опросника Asthma Control Test (5 вопросов по 1–5 баллов). */
@Entity(tableName = "act_results")
data class ActResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val q1: Int,
    val q2: Int,
    val q3: Int,
    val q4: Int,
    val q5: Int,
    val total: Int
)
