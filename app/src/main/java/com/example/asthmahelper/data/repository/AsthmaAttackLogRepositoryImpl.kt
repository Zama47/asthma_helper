package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.AsthmaAttackLogDao
import com.example.asthmahelper.data.db.entity.AsthmaAttackLogEntity
import com.example.asthmahelper.domain.model.AsthmaAttackLog
import com.example.asthmahelper.domain.model.AttackSeverity
import com.example.asthmahelper.domain.repository.AsthmaAttackLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AsthmaAttackLogRepositoryImpl @Inject constructor(
    private val dao: AsthmaAttackLogDao
) : AsthmaAttackLogRepository {

    override fun getAllAttacks(): Flow<List<AsthmaAttackLog>> {
        return dao.getAllAttacks().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun insertAttack(attack: AsthmaAttackLog): Long {
        return dao.insert(attack.toEntity())
    }

    override suspend fun updateAttack(attack: AsthmaAttackLog) {
        dao.update(attack.toEntity())
    }

    override suspend fun deleteAttack(attack: AsthmaAttackLog) {
        dao.delete(attack.toEntity())
    }

    private fun AsthmaAttackLogEntity.toDomain(): AsthmaAttackLog {
        return AsthmaAttackLog(
            id = id,
            timestamp = timestamp,
            severity = runCatching { AttackSeverity.valueOf(severity) }.getOrDefault(AttackSeverity.LOW),
            rescueDoses = rescueDoses,
            triggers = triggers.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            location = location,
            notes = notes
        )
    }

    private fun AsthmaAttackLog.toEntity(): AsthmaAttackLogEntity {
        return AsthmaAttackLogEntity(
            id = id,
            timestamp = timestamp,
            severity = severity.name,
            rescueDoses = rescueDoses,
            triggers = triggers.joinToString(","),
            location = location,
            notes = notes
        )
    }
}
