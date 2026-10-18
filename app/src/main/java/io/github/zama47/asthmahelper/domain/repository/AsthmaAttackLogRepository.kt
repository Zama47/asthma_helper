package io.github.zama47.asthmahelper.domain.repository

import io.github.zama47.asthmahelper.domain.model.AsthmaAttackLog
import kotlinx.coroutines.flow.Flow

interface AsthmaAttackLogRepository {
    fun getAllAttacks(): Flow<List<AsthmaAttackLog>>
    suspend fun insertAttack(attack: AsthmaAttackLog): Long
    suspend fun updateAttack(attack: AsthmaAttackLog)
    suspend fun deleteAttack(attack: AsthmaAttackLog)
}
