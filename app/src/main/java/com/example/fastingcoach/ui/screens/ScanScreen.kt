package com.example.fastingcoach.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fastingcoach.data.FoodItem
import com.example.fastingcoach.ui.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ScanScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { captured ->
        bitmap = captured
        result = emptyList()
        error = null
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Scan food", style = MaterialTheme.typography.headlineMedium)
        Text("Take a meal photo. AI identifies the foods and portion estimates; the backend then looks up nutrition data.")

        Button(onClick = { camera.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Take food photo")
        }

        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Food photo",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
            Button(
                onClick = {
                    analyzing = true
                    error = null
                    scope.launch {
                        vm.analyzeFood(image)
                            .onSuccess { result = it }
                            .onFailure { error = it.message ?: "Unable to analyze image." }
                        analyzing = false
                    }
                },
                enabled = !analyzing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (analyzing) "Analyzing…" else "Analyze meal")
            }
        }

        if (bitmap == null) {
            OutlinedButton(onClick = vm::addDemoMeal, modifier = Modifier.fillMaxWidth()) {
                Text("Add demo meal (test the tracker)")
            }
        }

        error?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Scan error", style = MaterialTheme.typography.titleMedium)
                    Text(it)
                    Text("For the emulator, start the included backend on port 8000. For a physical phone, change API_BASE_URL to your server HTTPS address.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (result.isNotEmpty()) {
            Text("Detected meal", style = MaterialTheme.typography.titleLarge)
            result.forEachIndexed { index, item ->
                EditableFoodCard(item) { updated ->
                    result = result.toMutableList().also { it[index] = updated }
                }
            }

            val totalCalories = result.sumOf { it.calories }
            val protein = result.sumOf { it.proteinG }
            val carbs = result.sumOf { it.carbsG }
            val fat = result.sumOf { it.fatG }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Meal total", style = MaterialTheme.typography.titleMedium)
                    Text("$totalCalories kcal", style = MaterialTheme.typography.headlineSmall)
                    Text("Protein ${protein.roundToInt()}g • Carbs ${carbs.roundToInt()}g • Fat ${fat.roundToInt()}g")
                }
            }
            Button(
                onClick = {
                    vm.addMeal(result)
                    result = emptyList()
                    bitmap = null
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add meal to today") }
        }

        Text("Photo-based portions are estimates. Always let the user correct serving size before saving.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EditableFoodCard(item: FoodItem, onUpdate: (FoodItem) -> Unit) {
    var gramsText by remember(item.name) { mutableStateOf(item.grams.roundToInt().toString()) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("${item.calories} kcal • ${item.proteinG.roundToInt()}g protein • ${item.carbsG.roundToInt()}g carbs • ${item.fatG.roundToInt()}g fat")
            OutlinedTextField(
                value = gramsText,
                onValueChange = { text ->
                    gramsText = text
                    val newGrams = text.toDoubleOrNull()
                    if (newGrams != null && newGrams > 0 && item.grams > 0) {
                        val scale = newGrams / item.grams
                        onUpdate(
                            item.copy(
                                grams = newGrams,
                                calories = (item.calories * scale).roundToInt(),
                                proteinG = item.proteinG * scale,
                                carbsG = item.carbsG * scale,
                                fatG = item.fatG * scale
                            )
                        )
                    }
                },
                label = { Text("Estimated grams") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            item.confidence?.let { Text("Recognition confidence: ${(it * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
