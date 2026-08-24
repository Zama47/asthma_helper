package com.example.asthmahelper.domain.model

data class BreathingNorm(
    val minNormal: Float,
    val maxNormal: Float,
    val formulaType: Int // 0 - manual, 1 - by age/height, etc.
)