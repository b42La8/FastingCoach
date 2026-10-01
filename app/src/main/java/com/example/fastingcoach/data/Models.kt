package com.example.fastingcoach.data

data class UserProfile(
    val age: Int = 35,
    val sex: Sex = Sex.MALE,
    val heightCm: Double = 175.0,
    val weightKg: Double = 80.0,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val goal: Goal = Goal.LOSE,
    val calorieTargetOverride: Int? = null,
    val stepGoal: Long = 10_000L,
    val fastingTargetHours: Int = 16
)

enum class Sex { MALE, FEMALE }
enum class Goal { LOSE, MAINTAIN, GAIN }
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), ACTIVE(1.725), VERY_ACTIVE(1.9)
}

data class MacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

data class FoodItem(
    val name: String,
    val grams: Double,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val confidence: Double? = null
)

data class Meal(
    val id: String,
    val timestampMillis: Long,
    val items: List<FoodItem>
) {
    val calories: Int get() = items.sumOf { it.calories }
    val proteinG: Double get() = items.sumOf { it.proteinG }
    val carbsG: Double get() = items.sumOf { it.carbsG }
    val fatG: Double get() = items.sumOf { it.fatG }
}

data class FastingSession(
    val startMillis: Long,
    val endMillis: Long,
    val targetHours: Int
) {
    val durationHours: Double get() = (endMillis - startMillis) / 3_600_000.0
    val completedGoal: Boolean get() = durationHours >= targetHours
}

data class DailySteps(
    val epochDay: Long,
    val steps: Long
)

data class WeeklyInsight(
    val title: String,
    val detail: String
)

data class WeeklyReport(
    val calorieTarget: Int,
    val weeklyCalorieTarget: Int,
    val caloriesConsumed: Int,
    val averageDailyCalories: Int,
    val caloriesOverTarget: Int,
    val averageProteinG: Int,
    val stepGoal: Long,
    val averageSteps: Long,
    val stepGoalDays: Int,
    val fastingGoalDays: Int,
    val averageFastHours: Double,
    val extraFoods: List<Pair<String, Int>>,
    val habits: List<WeeklyInsight>,
    val improvements: List<WeeklyInsight>
)
