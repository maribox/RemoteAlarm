package it.bosler.remotealarm.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import it.bosler.remotealarm.ui.navigation.ScreenType

@Composable
fun SettingsScreen() {
    Column {
        Text(ScreenType.Settings.name.asString())
    }
}