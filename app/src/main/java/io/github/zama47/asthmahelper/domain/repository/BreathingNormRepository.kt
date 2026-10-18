package io.github.zama47.asthmahelper.domain.repository

import io.github.zama47.asthmahelper.domain.model.BreathingNorm

interface BreathingNormRepository {
    suspend fun getBreathingNorm(): BreathingNorm?
    suspend fun insertBreathingNorm(norm: BreathingNorm): Long
    suspend fun updateBreathingNorm(norm: BreathingNorm)
}
