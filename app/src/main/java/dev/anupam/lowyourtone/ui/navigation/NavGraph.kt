package dev.anupam.lowyourtone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.anupam.lowyourtone.ui.screens.ActionPickerScreen
import dev.anupam.lowyourtone.ui.screens.AddEditWakeWordScreen
import dev.anupam.lowyourtone.ui.screens.HomeScreen
import dev.anupam.lowyourtone.ui.screens.HistoryScreen
import dev.anupam.lowyourtone.ui.screens.SettingsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController, viewModel = hiltViewModel())
        }
        composable(
            route = "add_edit?wakeWordId={wakeWordId}",
            arguments = listOf(navArgument("wakeWordId") { type = NavType.StringType; nullable = true })
        ) {
            AddEditWakeWordScreen(navController = navController, viewModel = hiltViewModel())
        }
        composable(
            route = "action_picker/{actionId}",
            arguments = listOf(navArgument("actionId") { type = NavType.StringType })
        ) {
            ActionPickerScreen(navController = navController)
        }
        composable("history") {
            HistoryScreen(navController = navController, viewModel = hiltViewModel())
        }
        composable("settings") {
            SettingsScreen(navController = navController, viewModel = hiltViewModel())
        }
    }
}
