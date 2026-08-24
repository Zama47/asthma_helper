package com.example.asthmahelper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.asthmahelper.data.db.AppDatabase
import com.example.asthmahelper.data.db.entity.MedicineEntity
import com.example.asthmahelper.notification.MedicationReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AsthmaHelperApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        seedPopularMedicines()
        scheduleMedicationReminders()
    }

    /**
     * Предзаполнение списка популярных лекарств при первом запуске.
     * Пользовательские лекарства (isPopular = false) не затрагиваются.
     */
    private fun seedPopularMedicines() {
        applicationScope.launch {
            val dao = database.medicineDao()
            if (dao.getPopularCount() > 0) return@launch // уже заполняли

            dao.insertAll(
                listOf(
                    MedicineEntity(name = "Сальбутамол", description = "Бронхорасширяющее (ингалятор скорой помощи)", isPopular = true),
                    MedicineEntity(name = "Будесонид", description = "Ингаляционный кортикостероид (Пульмикорт)", isPopular = true),
                    MedicineEntity(name = "Беклометазон", description = "Ингаляционный кортикостероид", isPopular = true),
                    MedicineEntity(name = "Флутиказон", description = "Ингаляционный кортикостероид (Фликсотид)", isPopular = true),
                    MedicineEntity(name = "Формотерол", description = "Длительно действующий бронхорасширяющий", isPopular = true),
                    MedicineEntity(name = "Сальметерол", description = "Длительно действующий бронхорасширяющий (Серевент)", isPopular = true),
                    MedicineEntity(name = "Ипратропий", description = "Холинолитик (Атровент)", isPopular = true),
                    MedicineEntity(name = "Монтелукаст", description = "Антилейкотриеновый препарат (Сингуляр)", isPopular = true),
                    MedicineEntity(name = "Теофиллин", description = "Бронхорасширяющее системное", isPopular = true),
                    MedicineEntity(name = "Преднизолон", description = "Системный кортикостероид (при обострении)", isPopular = true),
                    MedicineEntity(name = "Лоратадин", description = "Антигистаминное (Кларитин)", isPopular = true),
                    MedicineEntity(name = "Цетиризин", description = "Антигистаминное (Зиртек)", isPopular = true),
                    MedicineEntity(name = "Фексофенадин", description = "Антигистаминное (Телфаст)", isPopular = true),
                    MedicineEntity(name = "Амброксол", description = "Муколитик (Лазолван)", isPopular = true),
                    MedicineEntity(name = "Серетид", description = "Комбинированный: флутиказон + салметерол", isPopular = true),
                    MedicineEntity(name = "Симбикорт", description = "Комбинированный: будесонид + формотерол", isPopular = true)
                )
            )
        }
    }

    /**
     * Периодическая проверка расписаний: каждые 15 минут воркер проверяет,
     * не пора ли принимать лекарство, и показывает уведомление.
     * (Минимальный интервал PeriodicWorkRequest — 15 минут.)
     */
    private fun scheduleMedicationReminders() {
        val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MedicationReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
