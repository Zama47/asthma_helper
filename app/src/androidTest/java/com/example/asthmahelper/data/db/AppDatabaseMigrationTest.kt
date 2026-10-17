package com.example.asthmahelper.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Проверяет, что схема текущей версии создаётся и миграции доводят базу до неё.
 * При добавлении версии 4 — добавить тест 3 -> 4.
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

    @Test
    fun migratesFromV2ToCurrent() {
        helper.createDatabase(TEST_DB, 2).close()

        // Падает, если миграция не доводит схему до APP_DATABASE_VERSION.
        helper.runMigrationsAndValidate(TEST_DB, APP_DATABASE_VERSION, true, *ALL_MIGRATIONS)
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
