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

    @Test
    fun actAnswerScore_worstOptionIsOneAndBestIsFive() {
        assertEquals(1, actAnswerScore(0))
        assertEquals(2, actAnswerScore(1))
        assertEquals(3, actAnswerScore(2))
        assertEquals(4, actAnswerScore(3))
        assertEquals(5, actAnswerScore(4))
    }

    @Test
    fun actScores_bestAnswersGiveMaximumControl() {
        val scores = actScores(listOf(4, 4, 4, 4, 4))

        assertEquals(listOf(5, 5, 5, 5, 5), scores)
        assertEquals(25, actTotal(scores))
        assertEquals(ActLevel.WELL_CONTROLLED, actLevel(actTotal(scores)))
    }

    @Test
    fun actScores_worstAnswersGiveNoControl() {
        val scores = actScores(listOf(0, 0, 0, 0, 0))

        assertEquals(listOf(1, 1, 1, 1, 1), scores)
        assertEquals(5, actTotal(scores))
        assertEquals(ActLevel.UNCONTROLLED, actLevel(actTotal(scores)))
    }
}
