package io.github.zama47.asthmahelper.ui.act

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.zama47.asthmahelper.domain.clinical.actLevel
import io.github.zama47.asthmahelper.domain.clinical.actScores
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ActQuestion(val text: String, val options: List<String>)

private val ACT_QUESTIONS = listOf(
    ActQuestion(
        text = "Как часто за последние 4 недели астма мешала вам заниматься обычными делами?",
        options = listOf("Всё время", "Часто", "Иногда", "Редко", "Никогда")
    ),
    ActQuestion(
        text = "Как часто у вас была одышка?",
        options = listOf("Более 1 раза в день", "1 раз в день", "3–6 раз в неделю", "1–2 раза в неделю", "Ни разу")
    ),
    ActQuestion(
        text = "Как часто из-за астмы вы просыпались ночью или раньше обычного?",
        options = listOf(
            "4 и более ночей в неделю",
            "2–3 ночи в неделю",
            "1 ночь в неделю",
            "1–2 раза за месяц",
            "Ни разу"
        )
    ),
    ActQuestion(
        text = "Как часто вы использовали спасательный ингалятор?",
        options = listOf(
            "3 и более раз в день",
            "1–2 раза в день",
            "2–3 раза в неделю",
            "1 раз в неделю или реже",
            "Ни разу"
        )
    ),
    ActQuestion(
        text = "Как бы вы оценили контроль над астмой за последние 4 недели?",
        options = listOf("Совсем не контролировал", "Плохо", "Удовлетворительно", "Хорошо", "Полностью контролировал")
    )
)

@Composable
fun ActScreen(onClose: () -> Unit) {
    val viewModel: ActViewModel = hiltViewModel()
    val locale = Locale.forLanguageTag("ru")
    val history by viewModel.history.collectAsState()

    val selections = remember { mutableStateListOf<Int?>(null, null, null, null, null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var savedScore by remember { mutableStateOf(0) }

    val allAnswered = selections.all { it != null }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Закрыть")
            }
            Text(
                text = "Тест контроля астмы (ACT)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "5 вопросов о последних 4 неделях. Сумма 5–25 баллов: ≥20 — хороший контроль, " +
                        "16–19 — частичный, ≤15 — контроль отсутствует.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            itemsIndexed(ACT_QUESTIONS) { index, question ->
                QuestionCard(
                    number = index + 1,
                    question = question,
                    selectedOption = selections[index],
                    onSelect = { optionIndex -> selections[index] = optionIndex }
                )
            }

            item {
                Button(
                    onClick = {
                        val scores = actScores(selections.map { it ?: 0 })
                        viewModel.saveResult(scores)
                        savedScore = scores.sum()
                        showResultDialog = true
                        for (i in selections.indices) selections[i] = null
                    },
                    enabled = allAnswered,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить результат")
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "История тестов", style = MaterialTheme.typography.titleMedium)
                }
                items(history) { result ->
                    val level = actLevel(result.total)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(level.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${result.total} / 25 · ${level.label}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", locale).format(Date(result.timestamp)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { viewModel.deleteResult(result) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResultDialog) {
        val level = actLevel(savedScore)
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("Результат: $savedScore / 25") },
            text = { Text(text = "Уровень контроля: ${level.label}.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResultDialog = false
                        // Тест пройден — возвращаемся на главный экран.
                        onClose()
                    }
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                Button(onClick = { showResultDialog = false }) {
                    Text("Остаться")
                }
            }
        )
    }
}

@Composable
private fun QuestionCard(number: Int, question: ActQuestion, selectedOption: Int?, onSelect: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$number. ${question.text}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            question.options.forEachIndexed { optionIndex, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(optionIndex) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == optionIndex,
                        onClick = { onSelect(optionIndex) }
                    )
                    Text(text = option, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
