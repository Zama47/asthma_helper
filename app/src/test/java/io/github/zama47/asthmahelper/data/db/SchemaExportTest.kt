package io.github.zama47.asthmahelper.data.db

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Следит за тем, чтобы экспортированная схема Room была в актуальном состоянии.
 * Падает, если схему забыли закоммитить/перегенерировать после изменения entities.
 */
class SchemaExportTest {

    @Test
    fun schemaFileExistsForCurrentDatabaseVersion() {
        val schemaDir = File("schemas/io.github.zama47.asthmahelper.data.db.AppDatabase")
        assertTrue(
            "Каталог экспортированных схем Room не найден: ${schemaDir.absolutePath}",
            schemaDir.isDirectory
        )

        val expected = File(schemaDir, "$APP_DATABASE_VERSION.json")
        assertTrue(
            "Нет экспортированной схемы для версии $APP_DATABASE_VERSION (${expected.name}). " +
                "Запустите сборку, чтобы KSP сгенерировал схему, и закоммитьте её.",
            expected.isFile
        )

        val versionInFile = Regex("\"version\"\\s*:\\s*(\\d+)")
            .find(expected.readText())
            ?.groupValues
            ?.get(1)
            ?.toInt()

        assertTrue(
            "В ${expected.name} указана версия $versionInFile, а APP_DATABASE_VERSION = $APP_DATABASE_VERSION",
            versionInFile == APP_DATABASE_VERSION
        )
    }
}
