package com.example.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.asthmahelper.data.db.entity.BreathingNormEntity

@Dao
interface BreathingNormDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(norm: BreathingNormEntity): Long

    @Update
    suspend fun update(norm: BreathingNormEntity)

    @Query("SELECT * FROM breathing_norms WHERE id = 1 LIMIT 1")
    suspend fun getBreathingNorm(): BreathingNormEntity?
}
