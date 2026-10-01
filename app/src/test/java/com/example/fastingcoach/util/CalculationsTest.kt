package com.example.fastingcoach.util

import com.example.fastingcoach.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalculationsTest {
    @Test
    fun calorieTarget_isPositiveAndMacrosFit() {
        val target = Calculations.recommendedTargets(UserProfile(weightKg = 80.0))
        assertTrue(target.calories >= 1200)
        assertTrue(target.proteinG > 0)
        assertTrue(target.carbsG >= 0)
        assertTrue(target.fatG > 0)
    }

    @Test
    fun extraSteps_zeroWhenNotOver() {
        assertEquals(0L, Calculations.estimatedExtraSteps(0, 80.0))
    }

    @Test
    fun weeklyOverageAttributesOnlyExcessCalories() {
        val date = LocalDate.now()
        val noon = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val night = date.atTime(21, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val profile = UserProfile(calorieTargetOverride = 2000)
        val meals = listOf(
            Meal("1", noon, listOf(FoodItem("Meals", 1000.0, 1900, 50.0, 200.0, 50.0))),
            Meal("2", night, listOf(FoodItem("Snack", 100.0, 300, 5.0, 40.0, 10.0)))
        )
        val report = Calculations.buildWeeklyReport(profile, meals, emptyList(), emptyList(), date)
        assertEquals(200, report.caloriesOverTarget)
        assertEquals("Snack", report.extraFoods.first().first)
        assertEquals(200, report.extraFoods.first().second)
    }
}
