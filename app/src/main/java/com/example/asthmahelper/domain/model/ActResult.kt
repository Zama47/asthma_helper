package com.example.asthmahelper.domain.model

/** Сохранённый результат опросника Asthma Control Test. */
data class ActResult(val id: Long = 0, val timestamp: Long, val answers: List<Int>, val total: Int)
