package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.ActResultDao
import com.example.asthmahelper.data.db.entity.ActResultEntity
import com.example.asthmahelper.domain.clinical.actTotal
import com.example.asthmahelper.domain.model.ActResult
import com.example.asthmahelper.domain.repository.ActResultRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActResultRepositoryImpl @Inject constructor(private val dao: ActResultDao) : ActResultRepository {

    override fun getAllResults(): Flow<List<ActResult>> = dao.getAllResults().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun insertResult(result: ActResult): Long = dao.insert(result.toEntity())

    override suspend fun deleteResult(result: ActResult) {
        dao.delete(result.toEntity())
    }

    private fun ActResultEntity.toDomain(): ActResult = ActResult(
        id = id,
        timestamp = timestamp,
        answers = listOf(q1, q2, q3, q4, q5),
        total = total
    )

    private fun ActResult.toEntity(): ActResultEntity {
        val values = answers.ifEmpty { listOf(1, 1, 1, 1, 1) }
        return ActResultEntity(
            id = id,
            timestamp = timestamp,
            q1 = values.getOrElse(0) { 1 },
            q2 = values.getOrElse(1) { 1 },
            q3 = values.getOrElse(2) { 1 },
            q4 = values.getOrElse(3) { 1 },
            q5 = values.getOrElse(4) { 1 },
            total = actTotal(values)
        )
    }
}
