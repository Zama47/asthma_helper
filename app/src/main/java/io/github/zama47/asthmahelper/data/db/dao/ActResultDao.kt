package io.github.zama47.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.zama47.asthmahelper.data.db.entity.ActResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ActResultEntity): Long

    @Delete
    suspend fun delete(result: ActResultEntity)

    @Query("SELECT * FROM act_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<ActResultEntity>>
}
