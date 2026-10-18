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
import com.example.asthmahelper.domain.clinical.Zone
import com.example.asthmahelper.domain.clinical.personalBest
import com.example.asthmahelper.domain.clinical.zoneOf
import com.example.asthmahelper.domain.clinical.zoneOfPercent
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import com.example.asthmahelper.domain.repository.MedicineRepository
import com.example.asthmahelper.domain.usecase.ToggleMedicationTakenUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Виджет домашнего экрана: последний замер ПСВ с кружком зоны,
 * кнопка добавления замера и галочки приёма лекарств на сегодня.
 */
@AndroidEntryPoint
class PefWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var measurementRepository: DutyMeasurementRepository

    @Inject
    lateinit var normRepository: BreathingNormRepository

    @Inject
    lateinit var scheduleRepository: MedicationScheduleRepository

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var logRepository: MedicationLogRepository

    @Inject
    lateinit var toggleMedicationTaken: ToggleMedicationTakenUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_MEDICATION) {
            val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
            val taken = intent.getBooleanExtra(EXTRA_TAKEN, false)
            if (scheduleId > 0) {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        toggleMedicationTaken(scheduleId, System.currentTimeMillis(), taken)
                        refresh(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val state = loadState()
                for (widgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(widgetId, buildViews(context, state))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Добавили первый виджет — сразу показываем актуальные данные.
        refresh(context)
    }

    private suspend fun loadState(): WidgetState {
        val measurements = measurementRepository.getAllMeasurements().first()
        val latest = measurements.maxByOrNull { it.timestamp }
        val best = personalBest(measurements)
        val norm = normRepository.getBreathingNorm()
        val zone = when {
            latest == null -> null
            best != null -> zoneOfPercent(latest.value, best)
            else -> zoneOf(latest.value, norm)
        }

        val todayStart = startOfToday()
        val isoDay = isoDayOfWeek()
        val names = medicineRepository.getAllMedicines().first().associate { it.id to it.name }
        val schedules = scheduleRepository.getActiveSchedules().first()
            .filter { it.daysOfWeek.contains(isoDay.toString()) }
            .sortedBy { it.timeOfDay }
        val takenIds = logRepository.getLogsForDate(todayStart).first()
            .filter { it.taken }
            .map { it.scheduleId }
            .toSet()

        val medications = schedules.map { schedule ->
            MedicationRow(
                scheduleId = schedule.id,
                title = buildString {
                    append(names[schedule.medicineId] ?: "Лекарство")
                    if (schedule.dose.isNotBlank()) append(" · ").append(schedule.dose)
                },
                taken = schedule.id in takenIds
            )
        }

        return WidgetState(
            value = latest?.value,
            timestamp = latest?.timestamp,
            zone = zone,
            medications = medications
        )
    }

    private fun buildViews(context: Context, state: WidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_pef)

        views.setTextViewText(
            R.id.widget_value,
            state.value?.let { it.toInt().toString() } ?: "—"
        )
        views.setTextViewText(
            R.id.widget_zone,
            state.zone?.let { "${zoneTitle(it)} зона" } ?: "Нет данных"
        )
        views.setTextViewText(
            R.id.widget_date,
            state.timestamp?.let { DATE_FORMAT.format(Date(it)) } ?: ""
        )
        views.setTextColor(R.id.widget_zone_dot, zoneColor(state.zone))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_add, addMeasurementIntent(context))

        views.removeAllViews(R.id.widget_meds_container)
        views.setViewVisibility(
            R.id.widget_meds_empty,
            if (state.medications.isEmpty()) View.VISIBLE else View.GONE
        )
        state.medications.take(MAX_MEDICATION_ROWS).forEach { row ->
            views.addView(R.id.widget_meds_container, buildMedicationRow(context, row))
        }
        val hidden = state.medications.size - MAX_MEDICATION_ROWS
        if (hidden > 0) {
            views.addView(R.id.widget_meds_container, buildOverflowRow(context, hidden))
        }
        return views
    }

    private fun buildMedicationRow(context: Context, row: MedicationRow): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_medication_row)
        views.setTextViewText(R.id.widget_med_check, if (row.taken) "☑" else "☐")
        views.setTextColor(
            R.id.widget_med_check,
            if (row.taken) CHECKED_COLOR else UNCHECKED_COLOR
        )
        views.setTextViewText(R.id.widget_med_name, row.title)
        views.setOnClickPendingIntent(
            R.id.widget_med_row,
            toggleMedicationIntent(context, row.scheduleId, !row.taken)
        )
        return views
    }

    private fun buildOverflowRow(context: Context, hidden: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_medication_row)
        views.setViewVisibility(R.id.widget_med_check, View.GONE)
        views.setTextViewText(R.id.widget_med_name, "и ещё $hidden")
        return views
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Открывает приложение сразу с диалогом добавления замера. */
    private fun addMeasurementIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ADD_MEASUREMENT, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Тап по галочке лекарства — переключает приём прямо из виджета. */
    private fun toggleMedicationIntent(context: Context, scheduleId: Long, taken: Boolean): PendingIntent {
        val intent = Intent(context, PefWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_MEDICATION
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_TAKEN, taken)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
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

    private fun zoneTitle(zone: Zone): String = when (zone) {
        Zone.GREEN -> "Зелёная"
        Zone.YELLOW -> "Жёлтая"
        Zone.RED -> "Красная"
    }

    private fun zoneColor(zone: Zone?): Int = when (zone) {
        Zone.GREEN -> 0xFF4CAF50.toInt()
        Zone.YELLOW -> 0xFFF59E0B.toInt()
        Zone.RED -> 0xFFF44336.toInt()
        null -> 0xFF9E9E9E.toInt()
    }

    private data class MedicationRow(val scheduleId: Long, val title: String, val taken: Boolean)

    private data class WidgetState(
        val value: Float?,
        val timestamp: Long?,
        val zone: Zone?,
        val medications: List<MedicationRow>
    )

    companion object {
        private const val ACTION_TOGGLE_MEDICATION =
            "com.example.asthmahelper.widget.action.TOGGLE_MEDICATION"
        private const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        private const val EXTRA_TAKEN = "extra_taken"
        private const val MAX_MEDICATION_ROWS = 3
        private const val CHECKED_COLOR = 0xFF2E7D32.toInt()
        private const val UNCHECKED_COLOR = 0xFF90A4AE.toInt()
        private val DATE_FORMAT = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

        /** Просит систему перерисовать все виджеты (вызывается при открытии приложения). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PefWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, PefWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
