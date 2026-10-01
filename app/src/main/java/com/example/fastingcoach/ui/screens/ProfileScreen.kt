package com.example.fastingcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fastingcoach.data.*
import com.example.fastingcoach.ui.AppViewModel

@Composable
fun ProfileScreen(vm: AppViewModel, modifier: Modifier = Modifier, onConnectHealth: () -> Unit) {
    var p by remember(vm.profile) { mutableStateOf(vm.profile) }
    var age by remember(vm.profile) { mutableStateOf(p.age.toString()) }
    var height by remember(vm.profile) { mutableStateOf(p.heightCm.toInt().toString()) }
    var weight by remember(vm.profile) { mutableStateOf(p.weightKg.toString()) }
    var steps by remember(vm.profile) { mutableStateOf(p.stepGoal.toString()) }
    var fastHours by remember(vm.profile) { mutableStateOf(p.fastingTargetHours.toString()) }
    var customCalories by remember(vm.profile) { mutableStateOf(p.calorieTargetOverride?.toString() ?: "") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile & goals", style = MaterialTheme.typography.headlineMedium)
        Text("These values are used to estimate calorie, macro and walking targets.")

        NumberField("Age", age) { age = it }
        NumberField("Height (cm)", height) { height = it }
        NumberField("Weight (kg)", weight, decimal = true) { weight = it }

        Text("Sex used by calorie equation", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = p.sex == Sex.MALE, onClick = { p = p.copy(sex = Sex.MALE) }, label = { Text("Male") })
            FilterChip(selected = p.sex == Sex.FEMALE, onClick = { p = p.copy(sex = Sex.FEMALE) }, label = { Text("Female") })
        }

        Text("Goal", style = MaterialTheme.typography.titleMedium)
        Goal.entries.forEach { goal ->
            Row {
                RadioButton(selected = p.goal == goal, onClick = { p = p.copy(goal = goal) })
                Text(goal.name.lowercase().replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(top = 12.dp))
            }
        }

        Text("Activity level", style = MaterialTheme.typography.titleMedium)
        ActivityLevel.entries.forEach { level ->
            Row {
                RadioButton(selected = p.activityLevel == level, onClick = { p = p.copy(activityLevel = level) })
                Text(level.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(top = 12.dp))
            }
        }

        NumberField("Daily step goal", steps) { steps = it }
        NumberField("Fasting goal (hours)", fastHours) { fastHours = it }
        NumberField("Optional custom calorie target", customCalories) { customCalories = it }

        val preview = p.copy(
            age = age.toIntOrNull() ?: p.age,
            heightCm = height.toDoubleOrNull() ?: p.heightCm,
            weightKg = weight.toDoubleOrNull() ?: p.weightKg,
            stepGoal = steps.toLongOrNull() ?: p.stepGoal,
            fastingTargetHours = fastHours.toIntOrNull() ?: p.fastingTargetHours,
            calorieTargetOverride = customCalories.toIntOrNull()
        )
        val estimated = com.example.fastingcoach.util.Calculations.recommendedTargets(preview)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Estimated daily targets", style = MaterialTheme.typography.titleMedium)
                Text("${estimated.calories} kcal")
                Text("Protein ${estimated.proteinG}g • Carbs ${estimated.carbsG}g • Fat ${estimated.fatG}g")
            }
        }

        Button(
            onClick = { vm.updateProfile(preview); p = preview },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save profile") }

        Divider()
        Text("Samsung / Health Connect", style = MaterialTheme.typography.titleLarge)
        Text(if (vm.healthConnected) "Connected. Steps can be read from Health Connect." else "Not connected. Samsung Health can share supported step data through Health Connect.")
        Button(onClick = onConnectHealth, modifier = Modifier.fillMaxWidth()) {
            Text(if (vm.healthConnected) "Review step permission" else "Connect Health Connect")
        }

        Text("The calorie recommendation uses a standard BMR/TDEE estimate and is not appropriate for every medical situation. Users should be able to set their own target or consult a qualified clinician when needed.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NumberField(label: String, value: String, decimal: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
