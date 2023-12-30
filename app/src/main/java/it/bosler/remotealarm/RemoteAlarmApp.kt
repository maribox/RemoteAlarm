package it.bosler.remotealarm

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.bosler.remotealarm.model.UiText
import it.bosler.remotealarm.ui.components.AlarmCardListPreview
import it.bosler.remotealarm.ui.components.AlarmCardPreview
import it.bosler.remotealarm.ui.theme.RemoteAlarmTheme

sealed class ScreenType(
    val route: String,
    val name: UiText,
    val icon: ImageVector, // Assuming you are using ImageVector for icons
) {
    data object Alarms : ScreenType(
        "alarms",
        UiText.StringResource(R.string.alarms),
        Icons.Default.Alarm,
    )
    data object Connections : ScreenType(
        "connections",
        UiText.StringResource(R.string.connections),
        Icons.Default.SpeakerPhone,
    )
    data object Settings : ScreenType(
        "settings",
        UiText.StringResource(R.string.settings),
        Icons.Default.Settings,
    )

    companion object {
        val values: List<ScreenType>
            get() = listOf(Alarms, Connections, Settings)
    }
}


@Composable
fun RemoteAlarmApp() {
    RemoteAlarmTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val currentDestination = remember { mutableStateOf(ScreenType.Alarms.route) }

            // When going back, update the currentDestination
            navController.addOnDestinationChangedListener() { _,  destination, _ ->
                currentDestination.value = destination.route.toString()
            }

            Column {
                MainScreen(navController, modifier = Modifier.weight(1f))
                NavigationBar {
                    ScreenType.values.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, item.name.asString()) },
                            label = { Text(item.name.asString()) },
                            selected = item.route == currentDestination.value,
                            onClick = { navController.navigate(item.route); currentDestination.value = item.route}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController : NavHostController, modifier: Modifier = Modifier) {
    Box(modifier) {
        NavHost(navController = navController, startDestination = ScreenType.Alarms.route) {
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

@Composable
fun AlarmsScreen() {
    Column {
        Text(ScreenType.Alarms.name.asString())
        AlarmCardListPreview()
    }
}



@Composable
fun ConnectionsScreen() {
    Column {
        Text(ScreenType.Connections.name.asString())
    }
}

@Composable
fun SettingsScreen() {
    Column {
        Text(ScreenType.Settings.name.asString())
    }
}

