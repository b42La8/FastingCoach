package com.example.fastingcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fastingcoach.data.WeeklyInsight
import com.example.fastingcoach.ui.AppViewModel
import kotlin.math.roundToInt

@Composable
fun WeeklyScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val report = vm.weeklyReport()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weekly report", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = vm::refreshHealth) { Text("Refresh") }
        }
        Text("Last 7 days", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Scorecard", style = MaterialTheme.typography.titleLarge)
                Metric("Calories", "${report.caloriesConsumed} / ${report.weeklyCalorieTarget} kcal")
                Metric("Daily average", "${report.averageDailyCalories} kcal")
                Metric("Protein average", "${report.averageProteinG} g/day")
                Metric("Steps average", "${formatNumber(report.averageSteps)} / ${formatNumber(report.stepGoal)}")
                Metric("Step goal days", "${report.stepGoalDays} / 7")
                Metric("Fasting goal days", "${report.fastingGoalDays} / 7")
                if (report.averageFastHours > 0) Metric("Average fast", "${(report.averageFastHours * 10).roundToInt() / 10.0} hours")
                if (report.caloriesOverTarget > 0) {
                    Divider()
                    Text("${report.caloriesOverTarget} kcal above the 7-day target", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Text("Extra calorie contributors", style = MaterialTheme.typography.titleLarge)
        if (report.extraFoods.isEmpty()) {
            Text("No above-target day contributors found yet, or there is not enough meal data.")
        } else {
            report.extraFoods.forEach { (name, calories) ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, modifier = Modifier.weight(1f))
                        Text("$calories kcal logged")
                    }
                }
            }
            Text("These are foods logged on above-target days; this does not mean the foods themselves are 'bad'.", style = MaterialTheme.typography.bodySmall)
        }

        InsightSection("Eating habits", report.habits)
        InsightSection("Where to improve", report.improvements)

        Text("Insights are based only on what was logged and should be treated as wellness estimates, not diagnosis or medical advice.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun InsightSection(title: String, insights: List<WeeklyInsight>) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    insights.forEach { insight ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(insight.title, style = MaterialTheme.typography.titleMedium)
                Text(insight.detail)
            }
        }
    }
}
