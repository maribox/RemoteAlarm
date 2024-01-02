package it.bosler.remotealarm.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.bosler.remotealarm.ui.screens.AlarmsScreen
import it.bosler.remotealarm.ui.screens.ConnectionsScreen
import it.bosler.remotealarm.ui.screens.SettingsScreen
import it.bosler.remotealarm.viewmodel.AlarmEvent
import it.bosler.remotealarm.viewmodel.AlarmState

@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    alarmState: AlarmState,
    onAlarmEvent: (AlarmEvent) -> Unit
) {
    Box(modifier) {
        NavHost(navController = navController, startDestination = ScreenType.Alarms.route) {
            composable(ScreenType.Alarms.route) {
                AlarmsScreen(alarmState, onAlarmEvent)
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