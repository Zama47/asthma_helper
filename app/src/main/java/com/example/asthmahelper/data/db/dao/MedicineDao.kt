package com.example.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.asthmahelper.data.db.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: MedicineEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(medicines: List<MedicineEntity>)

    @Update
    suspend fun update(medicine: MedicineEntity)

    @Delete
    suspend fun delete(medicine: MedicineEntity)

    @Query("SELECT * FROM medicines ORDER BY name")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE isPopular = 1 ORDER BY name")
    fun getPopularMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE name LIKE :query ORDER BY name")
    fun searchMedicines(query: String): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Query("SELECT COUNT(*) FROM medicines WHERE isPopular = 1")
    suspend fun getPopularCount(): Int
}
