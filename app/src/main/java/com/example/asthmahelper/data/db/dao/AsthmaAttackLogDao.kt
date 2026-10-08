package com.example.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.asthmahelper.data.db.entity.AsthmaAttackLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AsthmaAttackLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attack: AsthmaAttackLogEntity): Long

    @Update
    suspend fun update(attack: AsthmaAttackLogEntity)

    @Delete
    suspend fun delete(attack: AsthmaAttackLogEntity)

    @Query("SELECT * FROM asthma_attacks ORDER BY timestamp DESC")
    fun getAllAttacks(): Flow<List<AsthmaAttackLogEntity>>
}
