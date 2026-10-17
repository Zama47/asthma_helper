package com.example.asthmahelper.domain.clinical

import org.junit.Assert.assertEquals
import org.junit.Test

class AsthmaControlTestTest {

    @Test
    fun actLevel_thresholds() {
        assertEquals(ActLevel.UNCONTROLLED, actLevel(5))
        assertEquals(ActLevel.UNCONTROLLED, actLevel(15))
        assertEquals(ActLevel.PARTIAL, actLevel(16))
        assertEquals(ActLevel.PARTIAL, actLevel(19))
        assertEquals(ActLevel.WELL_CONTROLLED, actLevel(20))
        assertEquals(ActLevel.WELL_CONTROLLED, actLevel(25))
    }

    @Test
    fun actTotal_sumsAnswers() {
        assertEquals(25, actTotal(listOf(5, 5, 5, 5, 5)))
        assertEquals(5, actTotal(listOf(1, 1, 1, 1, 1)))
    }

    @Test
    fun actTotal_clampsOutOfRangeAnswers() {
        assertEquals(9, actTotal(listOf(0, 9, 1, 1, -3)))
    }
}
