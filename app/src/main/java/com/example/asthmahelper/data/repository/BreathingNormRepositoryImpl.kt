package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.BreathingNormDao
import com.example.asthmahelper.data.db.entity.BreathingNormEntity
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import javax.inject.Inject

class BreathingNormRepositoryImpl @Inject constructor(
    private val dao: BreathingNormDao
) : BreathingNormRepository {

    override suspend fun getBreathingNorm(): BreathingNorm? {
        return dao.getBreathingNorm()?.toDomain()
    }

    override suspend fun insertBreathingNorm(norm: BreathingNorm): Long {
        return dao.insert(norm.toEntity())
    }

    override suspend fun updateBreathingNorm(norm: BreathingNorm) {
        dao.update(norm.toEntity())
    }

    private fun BreathingNormEntity.toDomain(): BreathingNorm {
        return BreathingNorm(
            minNormal = minNormal,
            maxNormal = maxNormal,
            formulaType = formulaType
        )
    }

    private fun BreathingNorm.toEntity(): BreathingNormEntity {
        return BreathingNormEntity(
            id = 1, // Always one record
            minNormal = minNormal,
            maxNormal = maxNormal,
            formulaType = formulaType
        )
    }
}