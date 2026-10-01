package com.example.fastingcoach.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("fasting_coach", Context.MODE_PRIVATE)

    fun loadProfile(): UserProfile {
        val raw = prefs.getString("profile", null) ?: return UserProfile()
        return runCatching {
            val o = JSONObject(raw)
            UserProfile(
                age = o.optInt("age", 35),
                sex = Sex.valueOf(o.optString("sex", Sex.MALE.name)),
                heightCm = o.optDouble("heightCm", 175.0),
                weightKg = o.optDouble("weightKg", 80.0),
                activityLevel = ActivityLevel.valueOf(o.optString("activityLevel", ActivityLevel.LIGHT.name)),
                goal = Goal.valueOf(o.optString("goal", Goal.LOSE.name)),
                calorieTargetOverride = if (o.isNull("calorieTargetOverride")) null else o.optInt("calorieTargetOverride"),
                stepGoal = o.optLong("stepGoal", 10_000),
                fastingTargetHours = o.optInt("fastingTargetHours", 16)
            )
        }.getOrDefault(UserProfile())
    }

    fun saveProfile(profile: UserProfile) {
        val o = JSONObject()
            .put("age", profile.age)
            .put("sex", profile.sex.name)
            .put("heightCm", profile.heightCm)
            .put("weightKg", profile.weightKg)
            .put("activityLevel", profile.activityLevel.name)
            .put("goal", profile.goal.name)
            .put("calorieTargetOverride", profile.calorieTargetOverride ?: JSONObject.NULL)
            .put("stepGoal", profile.stepGoal)
            .put("fastingTargetHours", profile.fastingTargetHours)
        prefs.edit().putString("profile", o.toString()).apply()
    }

    fun loadMeals(): List<Meal> {
        val raw = prefs.getString("meals", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val itemsArr = o.getJSONArray("items")
                    val items = buildList {
                        for (j in 0 until itemsArr.length()) {
                            val x = itemsArr.getJSONObject(j)
                            add(
                                FoodItem(
                                    name = x.getString("name"),
                                    grams = x.optDouble("grams", 0.0),
                                    calories = x.optInt("calories", 0),
                                    proteinG = x.optDouble("proteinG", 0.0),
                                    carbsG = x.optDouble("carbsG", 0.0),
                                    fatG = x.optDouble("fatG", 0.0),
                                    confidence = if (x.isNull("confidence")) null else x.optDouble("confidence")
                                )
                            )
                        }
                    }
                    add(Meal(o.getString("id"), o.getLong("timestampMillis"), items))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveMeals(meals: List<Meal>) {
        val arr = JSONArray()
        meals.forEach { meal ->
            val items = JSONArray()
            meal.items.forEach { item ->
                items.put(JSONObject()
                    .put("name", item.name)
                    .put("grams", item.grams)
                    .put("calories", item.calories)
                    .put("proteinG", item.proteinG)
                    .put("carbsG", item.carbsG)
                    .put("fatG", item.fatG)
                    .put("confidence", item.confidence ?: JSONObject.NULL))
            }
            arr.put(JSONObject()
                .put("id", meal.id)
                .put("timestampMillis", meal.timestampMillis)
                .put("items", items))
        }
        prefs.edit().putString("meals", arr.toString()).apply()
    }

    fun newMeal(items: List<FoodItem>, timestampMillis: Long = System.currentTimeMillis()): Meal =
        Meal(UUID.randomUUID().toString(), timestampMillis, items)

    fun loadFastingSessions(): List<FastingSession> {
        val raw = prefs.getString("fasts", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(FastingSession(o.getLong("startMillis"), o.getLong("endMillis"), o.getInt("targetHours")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveFastingSessions(sessions: List<FastingSession>) {
        val arr = JSONArray()
        sessions.forEach {
            arr.put(JSONObject()
                .put("startMillis", it.startMillis)
                .put("endMillis", it.endMillis)
                .put("targetHours", it.targetHours))
        }
        prefs.edit().putString("fasts", arr.toString()).apply()
    }

    fun loadActiveFastStart(): Long? = prefs.getLong("activeFastStart", -1L).takeIf { it > 0 }
    fun saveActiveFastStart(value: Long?) {
        if (value == null) prefs.edit().remove("activeFastStart").apply()
        else prefs.edit().putLong("activeFastStart", value).apply()
    }

    fun loadStepSnapshots(): List<DailySteps> {
        val raw = prefs.getString("steps", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(DailySteps(o.getLong("epochDay"), o.getLong("steps")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveStepSnapshots(items: List<DailySteps>) {
        val unique = items.associateBy { it.epochDay }.values.sortedBy { it.epochDay }.takeLast(60)
        val arr = JSONArray()
        unique.forEach { arr.put(JSONObject().put("epochDay", it.epochDay).put("steps", it.steps)) }
        prefs.edit().putString("steps", arr.toString()).apply()
    }
}
