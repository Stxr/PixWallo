package com.example.pixlwallo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pixlwallo.ui.screens.HomeScreen
import com.example.pixlwallo.ui.screens.PickerScreen
import com.example.pixlwallo.ui.screens.PreviewScreen
import com.example.pixlwallo.ui.screens.SettingsScreen

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { HomeScreen(nav) }
            composable("picker") { PickerScreen(nav) }
            composable("preview") { PreviewScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
        }
    }
}