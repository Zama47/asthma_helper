package com.example.asthmahelper.domain.model

enum class AttackSeverity { LOW, MEDIUM, HIGH }

data class AsthmaAttackLog(
    val id: Long = 0,
    val timestamp: Long,
    val severity: AttackSeverity,
    val rescueDoses: Int,
    val triggers: List<String>,
    val location: String? = null,
    val notes: String? = null
)
