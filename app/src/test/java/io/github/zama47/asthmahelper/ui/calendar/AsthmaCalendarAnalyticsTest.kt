package io.github.zama47.asthmahelper.ui.calendar

import io.github.zama47.asthmahelper.domain.model.AsthmaAttackLog
import io.github.zama47.asthmahelper.domain.model.AttackSeverity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsthmaCalendarAnalyticsTest {

    // 15 июня 2024, 12:00.
    private val now = at(2024, 5, 15, 12)

    @Test
    fun streak_minusOneWhenNoAttacks() {
        assertEquals(-1, calculateStreak(emptyList(), now))
    }

    @Test
    fun streak_zeroWhenAttackToday() {
        assertEquals(0, calculateStreak(listOf(attack(at(2024, 5, 15, 8))), now))
    }

    @Test
    fun streak_countsFullDaysSinceLastAttack() {
        assertEquals(3, calculateStreak(listOf(attack(at(2024, 5, 12, 8))), now))
    }

    @Test
    fun streak_usesLatestAttack() {
        val attacks = listOf(attack(at(2024, 5, 1, 8)), attack(at(2024, 5, 13, 8)))
        assertEquals(2, calculateStreak(attacks, now))
    }

    @Test
    fun nightAttack_isTrueBetweenMidnightAndSix() {
        assertTrue(isNightAttack(at(2024, 5, 15, 0)))
        assertTrue(isNightAttack(at(2024, 5, 15, 5)))
        assertFalse(isNightAttack(at(2024, 5, 15, 6)))
    }

    @Test
    fun monthNavigation_rollsOverYearBoundary() {
        assertEquals(MonthYear(2025, 0), nextMonth(MonthYear(2024, 11)))
        assertEquals(MonthYear(2023, 11), prevMonth(MonthYear(2024, 0)))
    }

    private fun attack(timestamp: Long) = AsthmaAttackLog(
        timestamp = timestamp,
        severity = AttackSeverity.LOW,
        rescueDoses = 0,
        triggers = emptyList()
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
