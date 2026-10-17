package com.example.asthmahelper.domain.clinical

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

    private fun measurement(value: Float, timestamp: Long) = DutyMeasurement(value = value, timestamp = timestamp)

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
