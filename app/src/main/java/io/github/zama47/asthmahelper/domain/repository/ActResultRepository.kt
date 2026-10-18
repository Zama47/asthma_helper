package io.github.zama47.asthmahelper.domain.repository

import io.github.zama47.asthmahelper.domain.model.ActResult
import kotlinx.coroutines.flow.Flow

interface ActResultRepository {
    fun getAllResults(): Flow<List<ActResult>>
    suspend fun insertResult(result: ActResult): Long
    suspend fun deleteResult(result: ActResult)
}
