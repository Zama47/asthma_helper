package com.example.asthmahelper.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: таблица календаря приступов астмы. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `asthma_attacks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`severity` TEXT NOT NULL, " +
                "`rescueDoses` INTEGER NOT NULL, " +
                "`triggers` TEXT NOT NULL, " +
                "`location` TEXT, " +
                "`notes` TEXT)"
        )
    }
}

/** v2 → v3: индексы на внешние ключи (убирает full table scan при изменении родителей). */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_medication_schedules_medicineId` " +
                "ON `medication_schedules` (`medicineId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_medication_logs_scheduleId` " +
                "ON `medication_logs` (`scheduleId`)"
        )
    }
}

/** v3 → v4: таблица результатов опросника Asthma Control Test. */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `act_results` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`q1` INTEGER NOT NULL, " +
                "`q2` INTEGER NOT NULL, " +
                "`q3` INTEGER NOT NULL, " +
                "`q4` INTEGER NOT NULL, " +
                "`q5` INTEGER NOT NULL, " +
                "`total` INTEGER NOT NULL)"
        )
    }
}

/** Все миграции схемы. Используется в DI и в тестах миграций. */
internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
