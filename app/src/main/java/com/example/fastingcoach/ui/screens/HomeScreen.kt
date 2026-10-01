package com.example.fastingcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fastingcoach.ui.AppViewModel
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: AppViewModel, modifier: Modifier = Modifier, onConnectHealth: () -> Unit) {
    val targets = vm.targets
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Text("Fasting, food and activity in one place.", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Calories", style = MaterialTheme.typography.titleMedium)
                Text("${vm.todayCalories} / ${targets.calories} kcal", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(
                    progress = { (vm.todayCalories.toFloat() / targets.calories.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (vm.caloriesOver > 0) {
                    Text("${vm.caloriesOver} kcal over today's target")
                    Text("Estimated additional walking equivalent: ${formatNumber(vm.estimatedExtraSteps)} steps")
                } else {
                    Text("${vm.caloriesRemaining} kcal remaining")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Macros", style = MaterialTheme.typography.titleMedium)
                Text("Protein  ${vm.todayProtein.roundToInt()} / ${targets.proteinG} g")
                Text("Carbs     ${vm.todayCarbs.roundToInt()} / ${targets.carbsG} g")
                Text("Fat          ${vm.todayFat.roundToInt()} / ${targets.fatG} g")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium)
                Text("${formatNumber(vm.stepsToday)} / ${formatNumber(vm.profile.stepGoal)}", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(
                    progress = { (vm.stepsToday.toFloat() / vm.profile.stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (vm.caloriesOver > 0) {
                    Text("Normal steps remaining: ${formatNumber(vm.normalStepsRemaining)}")
                    Text("Combined remaining estimate: ${formatNumber(vm.combinedStepsRemaining)}")
                }
                if (!vm.healthConnected) {
                    vm.healthMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = onConnectHealth) { Text("Connect Health Connect") }
                } else {
                    Text("Synced through Health Connect", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = vm::refreshHealth) { Text("Refresh steps") }
                }
            }
        }

        if (vm.todayMeals.isNotEmpty()) {
            Text("Today's meals", style = MaterialTheme.typography.titleLarge)
            vm.todayMeals.reversed().forEach { meal ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(meal.items.joinToString(", ") { it.name }, style = MaterialTheme.typography.titleSmall)
                        Text("${meal.calories} kcal • ${meal.proteinG.roundToInt()}g protein")
                        TextButton(onClick = { vm.deleteMeal(meal.id) }) { Text("Remove") }
                    }
                }
            }
        }

        Text(
            "Calorie and walking values are estimates for wellness tracking, not medical advice.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

internal fun formatNumber(value: Long): String = "%,d".format(value)
