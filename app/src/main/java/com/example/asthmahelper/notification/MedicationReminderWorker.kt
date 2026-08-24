package com.example.asthmahelper.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.asthmahelper.MainActivity
import com.example.asthmahelper.R
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * WorkManager-воркер: проверяет расписания и показывает уведомление,
 * если пора принимать лекарство (в пределах ±5 минут от текущего времени).
 */
@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleRepository: MedicationScheduleRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60L + now.get(Calendar.MINUTE)
        val dayOfWeek = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 0
        }

        val schedules: List<MedicationSchedule> =
            scheduleRepository.getActiveSchedules().first()

        schedules
            .filter { it.daysOfWeek.contains(dayOfWeek.toString()) }
            .filter { kotlin.math.abs(it.timeOfDay / 60_000 - nowMinutes) <= 5 }
            .forEach { schedule ->
                showNotification(schedule)
            }

        return Result.success()
    }

    private fun showNotification(schedule: MedicationSchedule) {
        val context = applicationContext

        // Проверка разрешения POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Пора принимать лекарство")
            .setContentText("Запланирован приём: ${schedule.dose}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(schedule.id.toInt(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о лекарствах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о запланированных приёмах лекарств"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val WORK_NAME = "medication_reminder"
    }
}
