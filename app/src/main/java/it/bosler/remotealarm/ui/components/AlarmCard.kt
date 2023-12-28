package it.bosler.remotealarm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.bosler.remotealarm.model.Alarm
import it.bosler.remotealarm.model.Days

@Composable
fun AlarmCard(alarm: Alarm) {
    Row (Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(alarm.name)
        Text(alarm.time)
        // print all days in alarm.days
        for (day in alarm.days) {
            Text(day.value.asString())
        }
    }
}


@Preview
@Composable
fun AlarmCardPreview() {
    AlarmCard(
        Alarm(
            0,
            "Alarm 1",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY)
        )
    )
}