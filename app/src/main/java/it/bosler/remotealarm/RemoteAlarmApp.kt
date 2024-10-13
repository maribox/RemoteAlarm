package it.bosler.remotealarm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import it.bosler.remotealarm.ui.navigation.MainScreen
import it.bosler.remotealarm.ui.navigation.ScreenType
import it.bosler.remotealarm.ui.theme.RemoteAlarmTheme
import it.bosler.remotealarm.ui.viewmodel.AlarmViewModel


@Composable
fun RemoteAlarmApp() {
    RemoteAlarmTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val currentDestination = rememberSaveable { mutableStateOf(ScreenType.Alarms.route) }

            // When going back, update the currentDestination
            navController.addOnDestinationChangedListener { _, destination, _ ->
                currentDestination.value = destination.route.toString()
            }

            Column {
                MainScreen(
                    navController,
                    modifier = Modifier.weight(1f),
                )
                NavigationBar {
                    ScreenType.values.forEach { item ->
                        NavigationBarItem(icon = { Icon(item.icon, item.name.asString()) },
                            label = { Text(item.name.asString()) },
                            selected = item.route == currentDestination.value,
                            onClick = {
                                navController.navigate(item.route)
                                currentDestination.value = item.route
                            })
                    }
                }
            }
        }
    }
}


