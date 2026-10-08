package com.example.asthmahelper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.asthmahelper.data.db.dao.AsthmaAttackLogDao
import com.example.asthmahelper.data.db.dao.BreathingNormDao
import com.example.asthmahelper.data.db.dao.DutyMeasurementDao
import com.example.asthmahelper.data.db.dao.MedicationLogDao
import com.example.asthmahelper.data.db.dao.MedicationScheduleDao
import com.example.asthmahelper.data.db.dao.MedicineDao
import com.example.asthmahelper.data.db.entity.AsthmaAttackLogEntity
import com.example.asthmahelper.data.db.entity.BreathingNormEntity
import com.example.asthmahelper.data.db.entity.DutyMeasurementEntity
import com.example.asthmahelper.data.db.entity.MedicationLogEntity
import com.example.asthmahelper.data.db.entity.MedicationScheduleEntity
import com.example.asthmahelper.data.db.entity.MedicineEntity

@Database(
    entities = [
        DutyMeasurementEntity::class,
        MedicineEntity::class,
        MedicationScheduleEntity::class,
        MedicationLogEntity::class,
        BreathingNormEntity::class,
        AsthmaAttackLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dutyMeasurementDao(): DutyMeasurementDao
    abstract fun medicineDao(): MedicineDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun breathingNormDao(): BreathingNormDao
    abstract fun asthmaAttackLogDao(): AsthmaAttackLogDao
}
