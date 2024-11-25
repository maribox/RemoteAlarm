package it.bosler.remotealarm.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.bosler.remotealarm.bluetooth.BluetoothManager
import it.bosler.remotealarm.ui.screens.AlarmsScreen
import it.bosler.remotealarm.ui.screens.ConnectionsScreen
import it.bosler.remotealarm.ui.screens.ControlScreen
import it.bosler.remotealarm.ui.screens.SettingsScreen
import it.bosler.remotealarm.ui.viewmodel.BluetoothViewModel
import it.bosler.remotealarm.ui.viewmodel.ControlViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val bluetoothManager = remember {BluetoothManager()}
    val factory = remember {ControlViewModel.get_factory(bluetoothManager)}
    val controlViewModel : ControlViewModel = viewModel(factory = factory)

    Box(modifier) {
        NavHost(navController = navController, startDestination = ScreenType.Control.route) {
            composable(ScreenType.Control.route) {
                ControlScreen(controlViewModel)
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

