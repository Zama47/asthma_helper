package com.example.asthmahelper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.asthmahelper.data.db.dao.ActResultDao
import com.example.asthmahelper.data.db.dao.AsthmaAttackLogDao
import com.example.asthmahelper.data.db.dao.BreathingNormDao
import com.example.asthmahelper.data.db.dao.DutyMeasurementDao
import com.example.asthmahelper.data.db.dao.MedicationLogDao
import com.example.asthmahelper.data.db.dao.MedicationScheduleDao
import com.example.asthmahelper.data.db.dao.MedicineDao
import com.example.asthmahelper.data.db.entity.ActResultEntity
import com.example.asthmahelper.data.db.entity.AsthmaAttackLogEntity
import com.example.asthmahelper.data.db.entity.BreathingNormEntity
import com.example.asthmahelper.data.db.entity.DutyMeasurementEntity
import com.example.asthmahelper.data.db.entity.MedicationLogEntity
import com.example.asthmahelper.data.db.entity.MedicationScheduleEntity
import com.example.asthmahelper.data.db.entity.MedicineEntity

/**
 * Версия схемы БД. Единственный источник правды: используется в @Database(version = ...)
 * и в тесте, который следит за тем, чтобы для каждой версии была экспортирована схема.
 * При изменении entities не забыть: bump версии + добавить Migration + запустить сборку,
 * чтобы сгенерировался новый schemas/<version>.json.
 */
const val APP_DATABASE_VERSION = 4

@Database(
    entities = [
        DutyMeasurementEntity::class,
        MedicineEntity::class,
        MedicationScheduleEntity::class,
        MedicationLogEntity::class,
        BreathingNormEntity::class,
        AsthmaAttackLogEntity::class,
        ActResultEntity::class
    ],
    version = APP_DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dutyMeasurementDao(): DutyMeasurementDao
    abstract fun medicineDao(): MedicineDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun breathingNormDao(): BreathingNormDao
    abstract fun asthmaAttackLogDao(): AsthmaAttackLogDao
    abstract fun actResultDao(): ActResultDao
}
