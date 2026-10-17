package com.example.asthmahelper.domain.clinical

/** Диапазон баллов опросника Asthma Control Test (ACT): 5 вопросов по 1–5 баллов. */
const val ACT_MIN_SCORE = 5
const val ACT_MAX_SCORE = 25

/** Уровень контроля астмы по сумме баллов ACT. */
enum class ActLevel {
    WELL_CONTROLLED,
    PARTIAL,
    UNCONTROLLED
}

/** Интерпретация ACT: ≥20 — контроль хороший, 16–19 — частичный, ≤15 — отсутствует. */
fun actLevel(total: Int): ActLevel = when {
    total >= 20 -> ActLevel.WELL_CONTROLLED
    total >= 16 -> ActLevel.PARTIAL
    else -> ActLevel.UNCONTROLLED
}

/** Сумма баллов по ответам (каждый 1–5). Некорректные значения зажимаются в диапазон вопросов. */
fun actTotal(answers: List<Int>): Int = answers.sumOf { it.coerceIn(1, 5) }
