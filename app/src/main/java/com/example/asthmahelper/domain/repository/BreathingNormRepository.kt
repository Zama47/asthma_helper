package com.example.asthmahelper.domain.repository

import com.example.asthmahelper.domain.model.BreathingNorm

interface BreathingNormRepository {
    suspend fun getBreathingNorm(): BreathingNorm?
    suspend fun insertBreathingNorm(norm: BreathingNorm): Long
    suspend fun updateBreathingNorm(norm: BreathingNorm)
}