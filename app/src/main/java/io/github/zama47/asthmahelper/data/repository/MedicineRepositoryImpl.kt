package io.github.zama47.asthmahelper.data.repository

import io.github.zama47.asthmahelper.data.db.dao.MedicineDao
import io.github.zama47.asthmahelper.data.db.entity.MedicineEntity
import io.github.zama47.asthmahelper.domain.model.Medicine
import io.github.zama47.asthmahelper.domain.repository.MedicineRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicineRepositoryImpl @Inject constructor(private val dao: MedicineDao) : MedicineRepository {

    override fun getAllMedicines(): Flow<List<Medicine>> = dao.getAllMedicines().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getPopularMedicines(): Flow<List<Medicine>> = dao.getPopularMedicines().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun searchMedicines(query: String): Flow<List<Medicine>> =
        dao.searchMedicines("%$query%").map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getMedicineById(id: Long): Medicine? = dao.getMedicineById(id)?.toDomain()

    override suspend fun insertMedicine(medicine: Medicine): Long = dao.insert(medicine.toEntity())

    override suspend fun updateMedicine(medicine: Medicine) {
        dao.update(medicine.toEntity())
    }

    override suspend fun deleteMedicine(medicine: Medicine) {
        dao.delete(medicine.toEntity())
    }

    private fun MedicineEntity.toDomain(): Medicine = Medicine(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        isPopular = isPopular
    )

    private fun Medicine.toEntity(): MedicineEntity = MedicineEntity(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        isPopular = isPopular
    )
}
