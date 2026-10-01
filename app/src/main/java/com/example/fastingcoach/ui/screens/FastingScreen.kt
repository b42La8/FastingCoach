package com.example.fastingcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fastingcoach.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FastingScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val start = vm.activeFastStart
    LaunchedEffect(start) {
        while (start != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val elapsed = if (start == null) 0L else (now - start).coerceAtLeast(0)
    val targetMillis = vm.profile.fastingTargetHours * 3_600_000L
    val fraction = (elapsed.toFloat() / targetMillis.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Fasting", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(if (start == null) "Ready to fast" else formatDuration(elapsed), style = MaterialTheme.typography.displaySmall)
                Text("Goal: ${vm.profile.fastingTargetHours} hours")
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                if (start == null) {
                    Button(onClick = vm::startFast) { Text("Start fast") }
                } else {
                    Button(onClick = vm::endFast) { Text("End fast") }
                }
            }
        }

        Text("What may be happening", style = MaterialTheme.typography.titleLarge)
        val hour = elapsed / 3_600_000.0
        val (title, detail) = when {
            start == null -> "Before you start" to "Choose a fasting schedule that fits your routine. Hydration and individual health needs matter."
            hour < 4 -> "Digestive phase" to "Your body is still processing and absorbing energy from recent meals."
            hour < 12 -> "Post-meal transition" to "Insulin generally declines between meals as the body draws more on stored energy."
            hour < 18 -> "Greater stored-energy use" to "As fasting continues, fat oxidation and ketone production may increase. Timing differs between people."
            else -> "Extended fasting period" to "Metabolic responses continue to shift with fasting duration, but exact timing and effects vary substantially by person."
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail)
            }
        }

        Text("Recent fasts", style = MaterialTheme.typography.titleLarge)
        if (vm.fastingSessions.isEmpty()) Text("No completed fasts yet.")
        vm.fastingSessions.takeLast(7).reversed().forEach {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(it.durationHours * 10).roundToInt() / 10.0} hours")
                    Text(if (it.completedGoal) "Goal reached" else "Ended early")
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
