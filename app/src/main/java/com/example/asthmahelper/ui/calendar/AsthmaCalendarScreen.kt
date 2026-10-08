package com.example.asthmahelper.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.asthmahelper.domain.model.AsthmaAttackLog
import com.example.asthmahelper.domain.model.AttackSeverity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val LowSeverityColor = Color(0xFFFFD166)
private val MediumSeverityColor = Color(0xFFF3722C)
private val HighSeverityColor = Color(0xFFF94144)

private fun severityColor(severity: AttackSeverity): Color = when (severity) {
    AttackSeverity.LOW -> LowSeverityColor
    AttackSeverity.MEDIUM -> MediumSeverityColor
    AttackSeverity.HIGH -> HighSeverityColor
}

private fun severityLabel(severity: AttackSeverity): String = when (severity) {
    AttackSeverity.LOW -> "Лёгкий"
    AttackSeverity.MEDIUM -> "Средний"
    AttackSeverity.HIGH -> "Тяжёлый"
}

private val TriggerPresets = listOf(
    "Пыльца", "Холодный воздух", "Физическая нагрузка", "Стресс",
    "Простуда / инфекция", "Пыль", "Дым / загрязнение", "Неизвестно"
)

private val LocationPresets = listOf("Дома", "На улице", "На работе", "Транспорт")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsthmaCalendarScreen(navController: androidx.navigation.NavController) {
    val viewModel: AsthmaCalendarViewModel = hiltViewModel()

    val attacks by viewModel.attacks.collectAsState()
    val month by viewModel.displayedMonth.collectAsState()
    val missedDays by viewModel.missedDays.collectAsState()

    var selectedDay by remember { mutableStateOf(startOfDayMillis(System.currentTimeMillis())) }
    var showDialog by remember { mutableStateOf(false) }
    var editingAttack by remember { mutableStateOf<AsthmaAttackLog?>(null) }
    var showFirstAid by remember { mutableStateOf(false) }
    val firstAidSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val streak = remember(attacks) { calculateStreak(attacks) }

    val severityByDay = remember(attacks, month) {
        val start = monthStartMillis(month)
        val end = monthEndMillis(month)
        attacks.filter { it.timestamp in start..end }
            .groupBy { startOfDayMillis(it.timestamp) }
            .mapValues { (_, list) -> list.maxOf { it.severity } }
    }

    val selectedDayAttacks = remember(attacks, selectedDay) {
        attacks
            .filter { startOfDayMillis(it.timestamp) == selectedDay }
            .sortedByDescending { it.timestamp }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Календарь приступов",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        editingAttack = null
                        showDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить приступ"
                        )
                    }
                }
            }

            item {
                StreakCard(
                    streak = streak,
                    currentMonthCount = attacks.count {
                        it.timestamp in monthStartMillis(currentMonth())..monthEndMillis(currentMonth())
                    },
                    previousMonthCount = attacks.count {
                        val prev = prevMonth(currentMonth())
                        it.timestamp in monthStartMillis(prev)..monthEndMillis(prev)
                    }
                )
            }

            item {
                MonthHeader(
                    month = month,
                    onPrev = {
                        val target = prevMonth(month)
                        viewModel.goToMonth(target)
                        selectedDay = resetSelectedDay(selectedDay, target)
                    },
                    onNext = {
                        val target = nextMonth(month)
                        viewModel.goToMonth(target)
                        selectedDay = resetSelectedDay(selectedDay, target)
                    }
                )
            }

            item { WeekdayHeader() }

            item {
                MonthGrid(
                    month = month,
                    selectedDay = selectedDay,
                    severityByDay = severityByDay,
                    missedDays = missedDays,
                    onDayClick = { day -> selectedDay = day }
                )
            }

            item { CalendarLegend() }

            item {
                Text(
                    text = "Приступы за " +
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            .format(Date(selectedDay)) +
                        " · ${selectedDayAttacks.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (selectedDayAttacks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "В этот день приступов не было",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Если приступ был — добавьте запись «+» или кнопкой «У меня приступ!»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(selectedDayAttacks, key = { it.id }) { attack ->
                    AttackCard(
                        attack = attack,
                        onClick = {
                            editingAttack = attack
                            showDialog = true
                        },
                        onDelete = { viewModel.deleteAttack(attack) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                viewModel.addSosAttack()
                // Прыгаем на сегодня, чтобы запись сразу была видна в календаре
                selectedDay = startOfDayMillis(System.currentTimeMillis())
                viewModel.goToMonth(currentMonth())
                showFirstAid = true
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            text = { Text("У меня приступ!") },
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }

    if (showDialog) {
        AttackDialog(
            initial = editingAttack,
            onDismiss = {
                showDialog = false
                editingAttack = null
            },
            onConfirm = { id, timestamp, severity, doses, triggers, location, notes ->
                viewModel.saveAttack(id, timestamp, severity, doses, triggers, location, notes)
                // Показываем день, к которому относится запись
                selectedDay = startOfDayMillis(timestamp)
                viewModel.goToMonth(monthOf(timestamp))
                showDialog = false
                editingAttack = null
            }
        )
    }

    if (showFirstAid) {
        ModalBottomSheet(
            onDismissRequest = { showFirstAid = false },
            sheetState = firstAidSheetState
        ) {
            FirstAidContent(onClose = { showFirstAid = false })
        }
    }
}

/** Первый день отображаемого месяца, если выбранный день в другой месяц. */
private fun resetSelectedDay(selectedDay: Long, month: MonthYear): Long {
    val start = monthStartMillis(month)
    val end = monthEndMillis(month)
    return if (selectedDay in start..end) selectedDay else start
}

// ---------- TIER 1: стрик ----------

@Composable
private fun StreakCard(streak: Int, currentMonthCount: Int, previousMonthCount: Int) {
    val colorScheme = MaterialTheme.colorScheme

    val cardColors = when {
        streak >= 14 -> CardDefaults.cardColors(
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer
        )
        streak in 0..2 -> CardDefaults.cardColors(
            containerColor = colorScheme.errorContainer,
            contentColor = colorScheme.onErrorContainer
        )
        streak < 0 -> CardDefaults.cardColors(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )
        else -> CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurface
        )
    }

    val message = when {
        streak >= 14 -> "Отличный результат! Ваша астма под контролем."
        streak in 0..2 -> "Будьте внимательны к симптомам."
        streak < 0 -> "Приступов пока не записано — так держать!"
        else -> "Хорошая динамика. Продолжайте вести журнал."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (streak >= 0) streak.toString() else "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (streak >= 0) {
                        "$streak ${daysPlural(streak)} без приступов"
                    } else {
                        "Приступы не записаны"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "В этом месяце: $currentMonthCount ${attacksPlural(currentMonthCount)}" +
                        " | В прошлом: $previousMonthCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- TIER 2: календарь ----------

@Composable
private fun MonthHeader(month: MonthYear, onPrev: () -> Unit, onNext: () -> Unit) {
    val title = remember(month) {
        SimpleDateFormat("LLLL yyyy", Locale("ru"))
            .format(Date(monthStartMillis(month)))
            .replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: MonthYear,
    selectedDay: Long,
    severityByDay: Map<Long, AttackSeverity>,
    missedDays: Set<Long>,
    onDayClick: (Long) -> Unit
) {
    val startMillis = monthStartMillis(month)
    val daysInMonth = remember(month) {
        Calendar.getInstance().apply { timeInMillis = startMillis }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val offset = remember(month) {
        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
        isoDayOfWeek(cal) - 1
    }
    val rowCount by remember(offset, daysInMonth) {
        derivedStateOf { (offset + daysInMonth + 6) / 7 }
    }
    val today = startOfDayMillis(System.currentTimeMillis())
    val gridHeightDp = rowCount * 52 + (rowCount - 1) * 4

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeightDp.dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(offset) { index ->
            Spacer(modifier = Modifier.height(52.dp))
        }
        items((1..daysInMonth).toList(), key = { it }) { day ->
            val dayMillis = startMillis + (day - 1) * 24L * 60 * 60 * 1000
            val normalizedDay = startOfDayMillis(dayMillis)
            DayCell(
                day = day,
                isToday = normalizedDay == today,
                isSelected = normalizedDay == selectedDay,
                severity = severityByDay[normalizedDay],
                missed = normalizedDay in missedDays,
                onClick = { onDayClick(normalizedDay) }
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    severity: AttackSeverity?,
    missed: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isSelected) colorScheme.primaryContainer else Color.Transparent,
                    CircleShape
                )
                .then(
                    if (isToday) {
                        Modifier.border(1.dp, colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        colorScheme.onPrimaryContainer
                    } else {
                        colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (severity != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(severityColor(severity), CircleShape)
                        )
                    }
                }
            }
        }

        if (missed) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "Пропущен приём лекарства",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(13.dp)
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(LowSeverityColor, "Лёгкий")
        LegendDot(MediumSeverityColor, "Средний")
        LegendDot(HighSeverityColor, "Тяжёлый")
        Icon(
            imageVector = Icons.Default.Medication,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "пропуск приёма",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- TIER 3: история дня ----------

@Composable
private fun AttackCard(
    attack: AsthmaAttackLog,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val color = severityColor(attack.severity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormat.format(Date(attack.timestamp)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(color, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = severityLabel(attack.severity),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    if (isNightAttack(attack.timestamp)) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Ночной приступ",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (attack.rescueDoses > 0) {
                    Text(
                        text = "Скорая помощь: ${attack.rescueDoses} вдох(ов)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (attack.triggers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        attack.triggers.forEach { trigger ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(trigger, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                attack.location?.let {
                    Text(
                        text = "Где: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                attack.notes?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ---------- Диалог добавления / редактирования приступа ----------

private fun parseDateTime(date: String, time: String): Long? = try {
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    format.isLenient = false
    format.parse("$date $time")?.time
} catch (e: Exception) {
    null
}

@Composable
private fun AttackDialog(
    initial: AsthmaAttackLog?,
    onDismiss: () -> Unit,
    onConfirm: (
        id: Long?,
        timestamp: Long,
        severity: AttackSeverity,
        rescueDoses: Int,
        triggers: List<String>,
        location: String?,
        notes: String
    ) -> Unit
) {
    val defaultTime = initial?.timestamp ?: System.currentTimeMillis()

    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(defaultTime)))
    }
    var timeText by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(defaultTime)))
    }
    var severity by remember { mutableStateOf(initial?.severity ?: AttackSeverity.MEDIUM) }
    var dosesText by remember { mutableStateOf((initial?.rescueDoses ?: 0).toString()) }
    var selectedTriggers by remember { mutableStateOf(initial?.triggers?.toSet() ?: setOf<String>()) }
    var location by remember { mutableStateOf(initial?.location) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    val dosesValid = dosesText.toIntOrNull()?.let { it in 0..99 } == true
    val timestamp = remember(dateText, timeText) { parseDateTime(dateText, timeText) }
    val valid = dosesValid && timestamp != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Новый приступ" else "Редактирование приступа")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Дата") },
                        placeholder = { Text("дд.мм.гггг") },
                        singleLine = true,
                        isError = timestamp == null,
                        supportingText = { Text("дд.мм.гггг") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Время") },
                        placeholder = { Text("чч:мм") },
                        singleLine = true,
                        isError = timestamp == null,
                        supportingText = { Text("чч:мм") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Тяжесть", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttackSeverity.values().forEach { s ->
                        FilterChip(
                            selected = severity == s,
                            onClick = { severity = s },
                            label = { Text(severityLabel(s)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = dosesText,
                    onValueChange = { dosesText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Вдохов скорой помощи") },
                    singleLine = true,
                    isError = !dosesValid,
                    supportingText = {
                        if (!dosesValid) Text("От 0 до 99")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Триггеры", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TriggerPresets.forEach { trigger ->
                        FilterChip(
                            selected = trigger in selectedTriggers,
                            onClick = {
                                selectedTriggers = if (trigger in selectedTriggers) {
                                    selectedTriggers - trigger
                                } else {
                                    selectedTriggers + trigger
                                }
                            },
                            label = { Text(trigger) }
                        )
                    }
                }

                Text("Где", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LocationPresets.forEach { place ->
                        FilterChip(
                            selected = location == place,
                            onClick = {
                                location = if (location == place) null else place
                            },
                            label = { Text(place) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметка") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        initial?.id,
                        timestamp ?: return@Button,
                        severity,
                        dosesText.toIntOrNull() ?: 0,
                        selectedTriggers.toList(),
                        location,
                        notes.trim()
                    )
                },
                enabled = valid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ---------- Первая помощь (GINA) ----------

@Composable
private fun FirstAidContent(onClose: () -> Unit) {
    val steps = listOf(
        "Сядьте прямо и успокойтесь" to
            "Не ложитесь, расстегните тесную одежду, откройте окно или выйдите на свежий воздух.",
        "Сделайте спокойный вдох" to
            "Вдохните носом, медленно выдохните губами «трубочкой». Повторите 3–5 раз.",
        "Примите лекарство" to
            "2–4 вдоха сальбутамола (скорая помощь) через спейсер, между вдохами пауза 30–60 секунд.",
        "Оцените состояние через 4 минуты" to
            "Если не легче — повторите. До 3 доз за первый час.",
        "Если не легче — вызовите скорую" to
            "Телефон 103 или 112. Не оставайтесь одни."
    )

    val emergency = listOf(
        "нет улучшений после 3 доз препарата скорой помощи",
        "губы, язык или ногти посинели",
        "трудно говорить, трудно дышать",
        "сонливость, спутанность сознания"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Первая помощь при приступе",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        steps.forEachIndexed { index, (title, description) ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Вызвать скорую (103 / 112), если:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                emergency.forEach {
                    Text(
                        text = "• $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Понятно")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
