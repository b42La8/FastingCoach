package com.example.fastingcoach.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fastingcoach.ui.screens.*

private enum class AppTab(val label: String, val icon: String) {
    HOME("Home", "⌂"), FAST("Fast", "◷"), SCAN("Scan", "◎"), WEEKLY("Weekly", "▥"), PROFILE("Profile", "●")
}

@Composable
fun FastingCoachApp(vm: AppViewModel = viewModel()) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        vm.onHealthPermissionResult(granted.containsAll(vm.health.requiredPermissions))
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.icon) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            AppTab.HOME -> HomeScreen(vm, modifier, onConnectHealth = { permissionLauncher.launch(vm.health.requiredPermissions) })
            AppTab.FAST -> FastingScreen(vm, modifier)
            AppTab.SCAN -> ScanScreen(vm, modifier)
            AppTab.WEEKLY -> WeeklyScreen(vm, modifier)
            AppTab.PROFILE -> ProfileScreen(vm, modifier, onConnectHealth = { permissionLauncher.launch(vm.health.requiredPermissions) })
        }
    }
}
