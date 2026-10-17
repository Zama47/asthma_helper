package com.example.asthmahelper.domain.repository

import com.example.asthmahelper.domain.model.ActResult
import kotlinx.coroutines.flow.Flow

interface ActResultRepository {
    fun getAllResults(): Flow<List<ActResult>>
    suspend fun insertResult(result: ActResult): Long
    suspend fun deleteResult(result: ActResult)
}
