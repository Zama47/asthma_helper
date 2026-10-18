package com.example.asthmahelper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.asthmahelper.MainActivity
import com.example.asthmahelper.R
import com.example.asthmahelper.domain.model.AsthmaAttackLog
import com.example.asthmahelper.domain.model.AttackSeverity
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.repository.AsthmaAttackLogRepository
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import com.example.asthmahelper.domain.usecase.ToggleMedicationTakenUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Виджет домашнего экрана из трёх кнопок: записать приступ,
 * добавить замер ПСВ и отметить приём лекарств.
 */
@AndroidEntryPoint
class AsthmaWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var scheduleRepository: MedicationScheduleRepository

    @Inject
    lateinit var logRepository: MedicationLogRepository

    @Inject
    lateinit var attackRepository: AsthmaAttackLogRepository

    @Inject
    lateinit var toggleMedicationTaken: ToggleMedicationTakenUseCase

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_LOG_ATTACK -> {
                runThenRefresh(context) { logAttack() }
                return
            }
            ACTION_TOGGLE_MEDICATION -> {
                val taken = intent.getBooleanExtra(EXTRA_TAKEN, false)
                runThenRefresh(context) { setAllMedicationsTaken(taken) }
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val views = buildViews(context, loadState())
                for (widgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Добавили первый виджет — сразу показываем актуальный статус.
        refresh(context)
    }

    private fun runThenRefresh(context: Context, block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                refresh(context)
                pendingResult.finish()
            }
        }
    }

    /** Однотапная запись приступа — так же, как кнопка «У меня приступ!» в приложении. */
    private suspend fun logAttack() {
        val now = System.currentTimeMillis()
        if (now - lastAttackTimestamp < ATTACK_DEBOUNCE_MS) return
        lastAttackTimestamp = now
        attackRepository.insertAttack(
            AsthmaAttackLog(
                timestamp = now,
                severity = AttackSeverity.MEDIUM,
                rescueDoses = 0,
                triggers = emptyList(),
                location = null,
                notes = "Отмечено из виджета"
            )
        )
    }

    /** Отмечает (или снимает отметку) приём всех лекарств, запланированных на сегодня. */
    private suspend fun setAllMedicationsTaken(taken: Boolean) {
        val now = System.currentTimeMillis()
        schedulesToday().forEach { schedule ->
            toggleMedicationTaken(schedule.id, now, taken)
        }
    }

    private suspend fun schedulesToday(): List<MedicationSchedule> {
        val isoDay = isoDayOfWeek()
        return scheduleRepository.getActiveSchedules().first()
            .filter { it.daysOfWeek.contains(isoDay.toString()) }
    }

    private suspend fun loadState(): WidgetState {
        val schedules = schedulesToday()
        val takenIds = logRepository.getLogsForDate(startOfToday()).first()
            .filter { it.taken }
            .map { it.scheduleId }
            .toSet()
        return WidgetState(
            hasMedicationsToday = schedules.isNotEmpty(),
            allTaken = schedules.isNotEmpty() && schedules.all { it.id in takenIds }
        )
    }

    private fun buildViews(context: Context, state: WidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

        views.setOnClickPendingIntent(R.id.widget_btn_attack, logAttackIntent(context))
        views.setOnClickPendingIntent(R.id.widget_btn_pef, addMeasurementIntent(context))

        // Кнопка лекарств активна только если на сегодня есть приёмы.
        if (state.hasMedicationsToday) {
            views.setOnClickPendingIntent(
                R.id.widget_btn_medication,
                toggleMedicationIntent(context, !state.allTaken)
            )
        }
        views.setViewVisibility(
            R.id.widget_med_bg_taken,
            if (state.allTaken) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widget_med_bg_pending,
            if (state.allTaken) View.GONE else View.VISIBLE
        )
        views.setImageViewResource(
            R.id.widget_med_icon,
            if (state.allTaken) R.drawable.ic_widget_med_taken else R.drawable.ic_widget_med_pending
        )
        return views
    }

    /** Записывает приступ, не открывая приложение. */
    private fun logAttackIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ATTACK,
        Intent(context, AsthmaWidgetProvider::class.java).setAction(ACTION_LOG_ATTACK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Открывает приложение сразу с диалогом добавления замера. */
    private fun addMeasurementIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ADD_MEASUREMENT, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_ADD_MEASUREMENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun toggleMedicationIntent(context: Context, taken: Boolean): PendingIntent {
        val intent = Intent(context, AsthmaWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_MEDICATION
            putExtra(EXTRA_TAKEN, taken)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_MEDICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun isoDayOfWeek(): Int = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }

    private data class WidgetState(val hasMedicationsToday: Boolean, val allTaken: Boolean)

    companion object {
        private const val ACTION_LOG_ATTACK = "com.example.asthmahelper.widget.action.LOG_ATTACK"
        private const val ACTION_TOGGLE_MEDICATION =
            "com.example.asthmahelper.widget.action.TOGGLE_MEDICATION"
        private const val EXTRA_TAKEN = "extra_taken"
        private const val REQUEST_ATTACK = 2
        private const val REQUEST_ADD_MEASUREMENT = 1
        private const val REQUEST_MEDICATION = 3
        private const val ATTACK_DEBOUNCE_MS = 3_000L

        /** Защита от случайного двойного тапа по «приступу». */
        private var lastAttackTimestamp = 0L

        /** Просит систему перерисовать все виджеты (вызывается при изменениях в приложении). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AsthmaWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, AsthmaWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
