package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.MedicineDao
import com.example.asthmahelper.data.db.entity.MedicineEntity
import com.example.asthmahelper.domain.model.Medicine
import com.example.asthmahelper.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicineRepositoryImpl @Inject constructor(
    private val dao: MedicineDao
) : MedicineRepository {

    override fun getAllMedicines(): Flow<List<Medicine>> {
        return dao.getAllMedicines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPopularMedicines(): Flow<List<Medicine>> {
        return dao.getPopularMedicines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchMedicines(query: String): Flow<List<Medicine>> {
        return dao.searchMedicines("%$query%").map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMedicineById(id: Long): Medicine? {
        return dao.getMedicineById(id)?.toDomain()
    }

    override suspend fun insertMedicine(medicine: Medicine): Long {
        return dao.insert(medicine.toEntity())
    }

    override suspend fun updateMedicine(medicine: Medicine) {
        dao.update(medicine.toEntity())
    }

    override suspend fun deleteMedicine(medicine: Medicine) {
        dao.delete(medicine.toEntity())
    }

    private fun MedicineEntity.toDomain(): Medicine {
        return Medicine(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            isPopular = isPopular
        )
    }

    private fun Medicine.toEntity(): MedicineEntity {
        return MedicineEntity(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            isPopular = isPopular
        )
    }
}