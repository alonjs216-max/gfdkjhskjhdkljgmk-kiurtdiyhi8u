package com.xaniihub.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCalorieCalculatorTest {
    @Test
    fun ordinaryWalkIsARealisticActiveEstimate() {
        val calories = StepCalorieCalculator.estimate(1324, 70f, 175, 0f)
        assertTrue(calories in 40f..80f)
    }

    @Test
    fun invalidCadenceCannotCreateAnomaly() {
        val zero = StepCalorieCalculator.estimate(1324, 70f, 175, 0f)
        val tiny = StepCalorieCalculator.estimate(1324, 70f, 175, 0.5f)
        val huge = StepCalorieCalculator.estimate(1324, 70f, 175, 10_000f)
        assertTrue(zero.isFinite() && tiny.isFinite() && huge.isFinite())
        assertTrue(zero < 100f && tiny < 100f && huge < 100f)
    }

    @Test
    fun stepsWeightAndHeightAffectEstimate() {
        assertEquals(0f, StepCalorieCalculator.estimate(0, 70f, 175, 100f), 0f)
        val base = StepCalorieCalculator.estimate(1324, 70f, 175, 100f)
        assertEquals(base * 2f, StepCalorieCalculator.estimate(1324, 140f, 175, 100f), 0.01f)
        assertTrue(StepCalorieCalculator.estimate(1324, 70f, 190, 100f) > base)
        assertEquals(0f, StepCalorieCalculator.estimate(1324, 0f, 175, 100f), 0f)
        assertEquals(0f, StepCalorieCalculator.estimate(1324, 70f, 0, 100f), 0f)
    }
}
