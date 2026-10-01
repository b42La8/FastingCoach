package com.example.fastingcoach.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastingcoach.BuildConfig
import com.example.fastingcoach.data.*
import com.example.fastingcoach.health.HealthConnectManager
import com.example.fastingcoach.network.FoodApiClient
import com.example.fastingcoach.util.Calculations
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppRepository(application)
    private val foodApi = FoodApiClient(BuildConfig.API_BASE_URL)
    val health = HealthConnectManager(application)

    var profile by mutableStateOf(repo.loadProfile())
        private set
    var meals by mutableStateOf(repo.loadMeals())
        private set
    var fastingSessions by mutableStateOf(repo.loadFastingSessions())
        private set
    var activeFastStart by mutableStateOf(repo.loadActiveFastStart())
        private set
    var stepSnapshots by mutableStateOf(repo.loadStepSnapshots())
        private set
    var stepsToday by mutableStateOf(stepSnapshots.firstOrNull { it.epochDay == LocalDate.now().toEpochDay() }?.steps ?: 0L)
        private set
    var healthConnected by mutableStateOf(false)
        private set
    var healthMessage by mutableStateOf<String?>(null)
        private set

    val targets: MacroTargets get() = Calculations.recommendedTargets(profile)
    val todayMeals: List<Meal> get() = Calculations.dailyMeals(meals, LocalDate.now())
    val todayCalories: Int get() = todayMeals.sumOf { it.calories }
    val todayProtein: Double get() = todayMeals.sumOf { it.proteinG }
    val todayCarbs: Double get() = todayMeals.sumOf { it.carbsG }
    val todayFat: Double get() = todayMeals.sumOf { it.fatG }
    val caloriesRemaining: Int get() = (targets.calories - todayCalories).coerceAtLeast(0)
    val caloriesOver: Int get() = (todayCalories - targets.calories).coerceAtLeast(0)
    val estimatedExtraSteps: Long get() = Calculations.estimatedExtraSteps(caloriesOver, profile.weightKg)
    val normalStepsRemaining: Long get() = (profile.stepGoal - stepsToday).coerceAtLeast(0)
    val combinedStepsRemaining: Long get() = normalStepsRemaining + estimatedExtraSteps

    init {
        refreshHealth()
    }

    fun updateProfile(value: UserProfile) {
        profile = value
        repo.saveProfile(value)
    }

    fun addMeal(items: List<FoodItem>) {
        if (items.isEmpty()) return
        meals = meals + repo.newMeal(items)
        repo.saveMeals(meals)
    }

    fun deleteMeal(id: String) {
        meals = meals.filterNot { it.id == id }
        repo.saveMeals(meals)
    }

    suspend fun analyzeFood(bitmap: Bitmap): Result<List<FoodItem>> = foodApi.analyze(bitmap)

    fun addDemoMeal() {
        addMeal(
            listOf(
                FoodItem("Grilled chicken breast", 170.0, 281, 53.0, 0.0, 6.0, 0.95),
                FoodItem("Cooked white rice", 190.0, 247, 5.0, 54.0, 1.0, 0.90),
                FoodItem("Mixed vegetables", 120.0, 70, 3.0, 13.0, 1.0, 0.80)
            )
        )
    }

    fun startFast() {
        if (activeFastStart != null) return
        activeFastStart = System.currentTimeMillis()
        repo.saveActiveFastStart(activeFastStart)
    }

    fun endFast() {
        val start = activeFastStart ?: return
        val end = System.currentTimeMillis()
        fastingSessions = fastingSessions + FastingSession(start, end, profile.fastingTargetHours)
        repo.saveFastingSessions(fastingSessions)
        activeFastStart = null
        repo.saveActiveFastStart(null)
    }

    fun refreshHealth() {
        viewModelScope.launch {
            runCatching {
                healthConnected = health.hasPermissions()
                if (healthConnected) {
                    val last7 = health.readLast7Days()
                    stepSnapshots = (stepSnapshots + last7).associateBy { it.epochDay }.values.sortedBy { it.epochDay }
                    repo.saveStepSnapshots(stepSnapshots)
                    stepsToday = stepSnapshots.firstOrNull { it.epochDay == LocalDate.now().toEpochDay() }?.steps ?: 0L
                    healthMessage = null
                } else {
                    healthMessage = "Connect Health Connect to import steps from Samsung Health or your Android device."
                }
            }.onFailure {
                healthConnected = false
                healthMessage = it.message ?: "Unable to read Health Connect."
            }
        }
    }

    fun onHealthPermissionResult(granted: Boolean) {
        healthConnected = granted
        if (granted) refreshHealth()
        else healthMessage = "Step permission was not granted. You can connect later from Profile."
    }

    fun weeklyReport(): WeeklyReport = Calculations.buildWeeklyReport(
        profile = profile,
        meals = meals,
        fastingSessions = fastingSessions,
        dailySteps = stepSnapshots
    )
}
