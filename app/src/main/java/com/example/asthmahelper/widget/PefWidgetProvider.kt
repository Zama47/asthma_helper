package com.example.asthmahelper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.asthmahelper.MainActivity
import com.example.asthmahelper.R
import com.example.asthmahelper.domain.clinical.Zone
import com.example.asthmahelper.domain.clinical.personalBest
import com.example.asthmahelper.domain.clinical.zoneOf
import com.example.asthmahelper.domain.clinical.zoneOfPercent
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Виджет домашнего экрана: последний замер ПСВ, зона и время. */
@AndroidEntryPoint
class PefWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var measurementRepository: DutyMeasurementRepository

    @Inject
    lateinit var normRepository: BreathingNormRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val measurements = measurementRepository.getAllMeasurements().first()
                val latest = measurements.maxByOrNull { it.timestamp }
                val best = personalBest(measurements)
                val norm = normRepository.getBreathingNorm()
                val zone = when {
                    latest == null -> null
                    best != null -> zoneOfPercent(latest.value, best)
                    else -> zoneOf(latest.value, norm)
                }
                for (widgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(
                        widgetId,
                        buildViews(context, latest?.value, latest?.timestamp, zone)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildViews(context: Context, value: Float?, timestamp: Long?, zone: Zone?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_pef)
        views.setTextViewText(
            R.id.widget_value,
            value?.let { it.toInt().toString() } ?: "—"
        )
        views.setTextViewText(
            R.id.widget_zone,
            zone?.let { "${zoneTitle(it)} зона" } ?: "Нет данных"
        )
        views.setTextViewText(
            R.id.widget_date,
            timestamp?.let { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(it)) } ?: ""
        )
        views.setTextColor(R.id.widget_value, zoneColor(zone))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_add, addMeasurementIntent(context))
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

    override fun onEnabled(context: Context) {
        // Добавили первый виджет — сразу показываем актуальные данные.
        refresh(context)
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

    companion object {
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
