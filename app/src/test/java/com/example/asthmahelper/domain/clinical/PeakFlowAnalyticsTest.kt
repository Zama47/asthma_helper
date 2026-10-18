package com.example.asthmahelper.domain.clinical

import com.example.asthmahelper.domain.model.AsthmaAttackLog
import com.example.asthmahelper.domain.model.AttackSeverity
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.model.DutyMeasurement
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeakFlowAnalyticsTest {

    // redZoneThreshold = 250, зелёная зона — от 400.
    private val norm = BreathingNorm(minNormal = 300f, maxNormal = 500f, formulaType = 0)

    @Test
    fun zone_withoutNorm_isGreen() {
        assertEquals(Zone.GREEN, zoneOf(100f, null))
    }

    @Test
    fun zone_atOrAboveEightyPercent_isGreen() {
        assertEquals(Zone.GREEN, zoneOf(400f, norm))
        assertEquals(Zone.GREEN, zoneOf(500f, norm))
    }

    @Test
    fun zone_betweenFiftyAndEightyPercent_isYellow() {
        assertEquals(Zone.YELLOW, zoneOf(399f, norm))
        assertEquals(Zone.YELLOW, zoneOf(250f, norm))
    }

    @Test
    fun zone_belowFiftyPercent_isRed() {
        assertEquals(Zone.RED, zoneOf(249f, norm))
    }

    @Test
    fun variability_isNullWithLessThanTwoMeasurementsPerDay() {
        assertNull(computeDailyVariability(listOf(measurement(400f, at(2024, 0, 10, 9)))))
        assertNull(computeDailyVariability(emptyList()))
    }

    @Test
    fun variability_usesFirstAndLastMeasurementOfDay() {
        val data = listOf(
            measurement(400f, at(2024, 0, 10, 8)),
            measurement(320f, at(2024, 0, 10, 20))
        )

        val result = computeDailyVariability(data)!!

        assertEquals(400f, result.morning, 0.001f)
        assertEquals(320f, result.evening, 0.001f)
        assertEquals(400f, result.max, 0.001f)
        assertEquals(20f, result.percent, 0.001f)
    }

    @Test
    fun variability_picksLatestDayWithData() {
        val data = listOf(
            measurement(400f, at(2024, 0, 10, 8)),
            measurement(200f, at(2024, 0, 10, 20)),
            measurement(500f, at(2024, 0, 12, 8)),
            measurement(450f, at(2024, 0, 12, 20))
        )

        assertEquals(500f, computeDailyVariability(data)!!.max, 0.001f)
    }

    @Test
    fun variabilityLevel_thresholds() {
        assertEquals(VariabilityLevel.NORMAL, variabilityLevel(14.9f))
        assertEquals(VariabilityLevel.BORDERLINE, variabilityLevel(15f))
        assertEquals(VariabilityLevel.BORDERLINE, variabilityLevel(20f))
        assertEquals(VariabilityLevel.HIGH, variabilityLevel(20.1f))
    }

    @Test
    fun personalBest_isNullWithoutMeasurementsInWindow() {
        assertNull(personalBest(emptyList(), now = at(2024, 0, 15, 12)))
        assertNull(personalBest(listOf(measurement(650f, at(2023, 0, 1, 10))), now = at(2024, 0, 15, 12)))
    }

    @Test
    fun personalBest_returnsMaxWithinWindow() {
        val now = at(2024, 0, 15, 12)
        val data = listOf(
            measurement(400f, at(2024, 0, 10, 8)),
            measurement(620f, at(2024, 0, 12, 8)),
            measurement(510f, at(2024, 0, 14, 8))
        )

        assertEquals(620f, personalBest(data, now = now)!!, 0.001f)
    }

    @Test
    fun personalBest_ignoresMeasurementsOlderThanWindow() {
        val now = at(2024, 0, 15, 12)
        val data = listOf(
            measurement(900f, at(2023, 11, 1, 8)),
            measurement(600f, at(2024, 0, 14, 8))
        )

        assertEquals(600f, personalBest(data, now = now)!!, 0.001f)
    }

    @Test
    fun personalBest_respectsCustomWindow() {
        val now = at(2024, 0, 15, 12)
        val data = listOf(
            measurement(900f, at(2024, 0, 13, 8)),
            measurement(500f, at(2024, 0, 15, 8))
        )

        assertEquals(500f, personalBest(data, windowDays = 1, now = now)!!, 0.001f)
    }

    @Test
    fun percentOfBest_computesShareOfMaximum() {
        assertEquals(80f, percentOfBest(400f, 500f)!!, 0.001f)
        assertEquals(100f, percentOfBest(500f, 500f)!!, 0.001f)
    }

    @Test
    fun percentOfBest_isNullWhenBestUnknownOrZero() {
        assertNull(percentOfBest(400f, null))
        assertNull(percentOfBest(400f, 0f))
    }

    @Test
    fun zoneOfPercent_usesGinaThresholds() {
        assertEquals(Zone.GREEN, zoneOfPercent(400f, 500f))
        assertEquals(Zone.YELLOW, zoneOfPercent(399f, 500f))
        assertEquals(Zone.YELLOW, zoneOfPercent(250f, 500f))
        assertEquals(Zone.RED, zoneOfPercent(249f, 500f))
    }

    @Test
    fun zoneOfPercent_isGreenWithoutBest() {
        assertEquals(Zone.GREEN, zoneOfPercent(100f, null))
    }

    @Test
    fun exacerbation_isNullWithoutBest() {
        assertNull(detectExacerbation(emptyList(), null, now = at(2024, 0, 15, 12)))
    }

    @Test
    fun exacerbation_detectsConsecutiveDaysBelowThreshold() {
        val now = at(2024, 0, 15, 18)
        val data = listOf(
            measurement(500f, at(2024, 0, 13, 8)),
            measurement(380f, at(2024, 0, 14, 8)),
            measurement(370f, at(2024, 0, 15, 8))
        )

        val warning = detectExacerbation(data, best = 500f, now = now)!!

        assertEquals(2, warning.consecutiveDays)
        assertEquals(74f, warning.worstPercentOfBest, 0.001f)
        assertEquals(at(2024, 0, 14, 0), warning.sinceMillis)
    }

    @Test
    fun exacerbation_isNullForSingleBadDay() {
        val now = at(2024, 0, 15, 18)
        val data = listOf(
            measurement(500f, at(2024, 0, 14, 8)),
            measurement(300f, at(2024, 0, 15, 8))
        )

        assertNull(detectExacerbation(data, best = 500f, now = now))
    }

    @Test
    fun exacerbation_isNullForStaleData() {
        val now = at(2024, 0, 15, 18)
        val data = listOf(
            measurement(300f, at(2024, 0, 5, 8)),
            measurement(300f, at(2024, 0, 6, 8))
        )

        assertNull(detectExacerbation(data, best = 500f, now = now))
    }

    @Test
    fun rescueDoseStats_sumsTodayAndWeek() {
        val now = at(2024, 0, 15, 18)
        val attacks = listOf(
            attack(2, at(2024, 0, 15, 9)),
            attack(3, at(2024, 0, 13, 9)),
            attack(4, at(2024, 0, 1, 9))
        )

        val stats = rescueDoseStats(attacks, now = now)

        assertEquals(2, stats.today)
        assertEquals(5, stats.thisWeek)
    }

    @Test
    fun isRescueUseHigh_threshold() {
        assertEquals(false, isRescueUseHigh(7))
        assertEquals(true, isRescueUseHigh(8))
    }

    private fun attack(doses: Int, timestamp: Long) = AsthmaAttackLog(
        rescueDoses = doses,
        timestamp = timestamp,
        severity = AttackSeverity.LOW,
        triggers = emptyList()
    )

    private fun measurement(value: Float, timestamp: Long) = DutyMeasurement(value = value, timestamp = timestamp)

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
