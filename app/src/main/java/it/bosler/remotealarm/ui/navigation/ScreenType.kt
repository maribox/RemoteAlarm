package it.bosler.remotealarm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.ui.graphics.vector.ImageVector
import it.bosler.remotealarm.R
import it.bosler.remotealarm.model.UiText

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