package com.example.fastingcoach.util

import com.example.fastingcoach.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Calculations {
    fun recommendedTargets(profile: UserProfile): MacroTargets {
        val sexAdjustment = if (profile.sex == Sex.MALE) 5.0 else -161.0
        val bmr = (10.0 * profile.weightKg) + (6.25 * profile.heightCm) - (5.0 * profile.age) + sexAdjustment
        val maintenance = bmr * profile.activityLevel.multiplier
        val goalAdjustment = when (profile.goal) {
            Goal.LOSE -> -500.0
            Goal.MAINTAIN -> 0.0
            Goal.GAIN -> 300.0
        }
        val suggestedCalories = (maintenance + goalAdjustment).toInt().coerceAtLeast(1_200)
        val calories = profile.calorieTargetOverride ?: suggestedCalories

        val protein = (profile.weightKg * 1.6).toInt().coerceAtLeast(60)
        val fat = ((calories * 0.25) / 9.0).toInt().coerceAtLeast(40)
        val remainingCalories = (calories - protein * 4 - fat * 9).coerceAtLeast(0)
        val carbs = remainingCalories / 4
        return MacroTargets(calories, protein, carbs, fat)
    }

    fun estimatedExtraSteps(excessCalories: Int, weightKg: Double): Long {
        if (excessCalories <= 0) return 0
        // Deliberately shown as an estimate in the UI. Roughly 0.04 kcal/step at 70 kg,
        // scaled by body weight. Real burn varies by pace, terrain, stride and physiology.
        val kcalPerStep = 0.04 * (weightKg / 70.0)
        return (excessCalories / kcalPerStep).toLong().coerceAtLeast(0)
    }

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun isOnDate(timestampMillis: Long, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate() == date

    fun dailyMeals(meals: List<Meal>, date: LocalDate): List<Meal> = meals.filter { isOnDate(it.timestampMillis, date) }

    fun buildWeeklyReport(
        profile: UserProfile,
        meals: List<Meal>,
        fastingSessions: List<FastingSession>,
        dailySteps: List<DailySteps>,
        endDate: LocalDate = LocalDate.now()
    ): WeeklyReport {
        val targets = recommendedTargets(profile)
        val dates = (0L..6L).map { endDate.minusDays(it) }
        val startDate = endDate.minusDays(6)

        val weekMeals = meals.filter {
            val d = Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            !d.isBefore(startDate) && !d.isAfter(endDate)
        }
        val consumed = weekMeals.sumOf { it.calories }
        val weeklyTarget = targets.calories * 7
        val avgCalories = consumed / 7
        val avgProtein = (weekMeals.sumOf { it.proteinG } / 7.0).toInt()

        val stepMap = dailySteps.associateBy { LocalDate.ofEpochDay(it.epochDay) }
        val weekSteps = dates.map { stepMap[it]?.steps ?: 0L }
        val avgSteps = if (weekSteps.isEmpty()) 0 else weekSteps.average().toLong()
        val stepGoalDays = weekSteps.count { it >= profile.stepGoal }

        val weekFasts = fastingSessions.filter {
            val d = Instant.ofEpochMilli(it.endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            !d.isBefore(startDate) && !d.isAfter(endDate)
        }
        val fastingGoalDays = weekFasts.count { it.completedGoal }
        val avgFastHours = if (weekFasts.isEmpty()) 0.0 else weekFasts.map { it.durationHours }.average()

        val overageFoodCalories = mutableMapOf<String, Int>()
        dates.forEach { date ->
            val dayMeals = dailyMeals(weekMeals, date).sortedBy { it.timestampMillis }
            val dayTotal = dayMeals.sumOf { it.calories }
            var remainingOverage = (dayTotal - targets.calories).coerceAtLeast(0)
            // Attribute only the calories beyond the daily target, starting with the latest logged foods.
            // This is a bookkeeping attribution, not a judgment that a particular food is inherently "bad".
            dayMeals.asReversed().flatMap { it.items.asReversed() }.forEach { item ->
                if (remainingOverage > 0) {
                    val attributed = minOf(item.calories, remainingOverage)
                    overageFoodCalories[item.name] = (overageFoodCalories[item.name] ?: 0) + attributed
                    remainingOverage -= attributed
                }
            }
        }
        val extraFoods = overageFoodCalories.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }

        val lateCalories = weekMeals.filter {
            val hour = Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).hour
            hour >= 20
        }.sumOf { it.calories }
        val lateShare = if (consumed == 0) 0.0 else lateCalories.toDouble() / consumed

        val weekdayCalories = weekMeals.filter {
            val dow = Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).dayOfWeek.value
            dow in 1..5
        }.sumOf { it.calories }
        val weekendCalories = weekMeals.filter {
            val dow = Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).dayOfWeek.value
            dow >= 6
        }.sumOf { it.calories }
        val weekdayDailyAvg = weekdayCalories / 5.0
        val weekendDailyAvg = weekendCalories / 2.0

        val habits = mutableListOf<WeeklyInsight>()
        if (consumed == 0) {
            habits += WeeklyInsight("Not enough meal data", "Log meals throughout the week to unlock calorie and eating-pattern insights.")
        } else {
            if (lateShare >= 0.25) habits += WeeklyInsight("Evening intake", "About ${(lateShare * 100).toInt()}% of logged calories were eaten after 8 PM.")
            if (weekdayDailyAvg > 0 && weekendDailyAvg > weekdayDailyAvg * 1.15) {
                val pct = (((weekendDailyAvg / weekdayDailyAvg) - 1) * 100).toInt()
                habits += WeeklyInsight("Weekend pattern", "Weekend intake averaged about $pct% higher than weekdays.")
            }
            val repeated = weekMeals.flatMap { it.items }.groupingBy { it.name.lowercase() }.eachCount().entries.maxByOrNull { it.value }
            if (repeated != null && repeated.value >= 3) habits += WeeklyInsight("Frequent food", "${repeated.key.replaceFirstChar { it.uppercase() }} appeared ${repeated.value} times in your food log.")
            if (habits.isEmpty()) habits += WeeklyInsight("Balanced timing", "No strong late-night or weekend calorie pattern stood out in the logged data this week.")
        }

        val improvements = mutableListOf<WeeklyInsight>()
        val over = (consumed - weeklyTarget).coerceAtLeast(0)
        if (over > 0) improvements += WeeklyInsight("Calories", "You logged $over kcal above the 7-day target. Look first at the meals listed under 'Extra calorie contributors'.")
        if (avgProtein in 1 until targets.proteinG) improvements += WeeklyInsight("Protein", "Average protein was ${avgProtein}g/day versus the ${targets.proteinG}g/day estimate. Consider shifting more protein into earlier meals.")
        if (avgSteps in 1 until profile.stepGoal) improvements += WeeklyInsight("Steps", "Average steps were $avgSteps/day. About ${profile.stepGoal - avgSteps} more steps per day would reach the current step goal.")
        if (weekFasts.isNotEmpty() && fastingGoalDays < weekFasts.size) improvements += WeeklyInsight("Fasting consistency", "$fastingGoalDays of ${weekFasts.size} completed fasts reached the ${profile.fastingTargetHours}-hour target.")
        if (improvements.isEmpty()) improvements += WeeklyInsight("Keep consistency", "Your logged data is close to the current targets. Focus on maintaining the routine rather than making large changes.")

        return WeeklyReport(
            calorieTarget = targets.calories,
            weeklyCalorieTarget = weeklyTarget,
            caloriesConsumed = consumed,
            averageDailyCalories = avgCalories,
            caloriesOverTarget = over,
            averageProteinG = avgProtein,
            stepGoal = profile.stepGoal,
            averageSteps = avgSteps,
            stepGoalDays = stepGoalDays,
            fastingGoalDays = fastingGoalDays,
            averageFastHours = avgFastHours,
            extraFoods = extraFoods,
            habits = habits,
            improvements = improvements
        )
    }
}
