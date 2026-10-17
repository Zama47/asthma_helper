package com.example.asthmahelper.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Проверяет, что схема текущей версии создаётся и что экспортированная схема валидна.
 * Когда появится версия 3, сюда добавятся тесты миграций 2 -> 3 и т.д.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun createsSchemaAtCurrentVersion() {
        val db = helper.createDatabase(TEST_DB, APP_DATABASE_VERSION)
        try {
            db.execSQL(
                "INSERT INTO duty_measurements (value, timestamp, note) VALUES (450.0, 1000, NULL)"
            )
            db.query("SELECT value FROM duty_measurements").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(450.0f, cursor.getFloat(0), 0.001f)
            }
        } finally {
            db.close()
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
