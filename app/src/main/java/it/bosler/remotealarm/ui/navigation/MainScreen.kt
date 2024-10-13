package it.bosler.remotealarm.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.bosler.remotealarm.ui.screens.AlarmsScreen
import it.bosler.remotealarm.ui.screens.ConnectionsScreen
import it.bosler.remotealarm.ui.screens.ControlScreen
import it.bosler.remotealarm.ui.screens.SettingsScreen

@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        NavHost(navController = navController, startDestination = ScreenType.Alarms.route) {
            composable(ScreenType.Control.route) {
                ControlScreen()
            }
            composable(ScreenType.Alarms.route) {
                AlarmsScreen()
            }
            composable(ScreenType.Connections.route) {
                ConnectionsScreen()
            }
            composable(ScreenType.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

