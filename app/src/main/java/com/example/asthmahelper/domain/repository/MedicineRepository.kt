package com.example.asthmahelper.domain.repository

import com.example.asthmahelper.domain.model.Medicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun getAllMedicines(): Flow<List<Medicine>>
    fun getPopularMedicines(): Flow<List<Medicine>>
    fun searchMedicines(query: String): Flow<List<Medicine>>
    suspend fun getMedicineById(id: Long): Medicine?
    suspend fun insertMedicine(medicine: Medicine): Long
    suspend fun updateMedicine(medicine: Medicine)
    suspend fun deleteMedicine(medicine: Medicine)
}