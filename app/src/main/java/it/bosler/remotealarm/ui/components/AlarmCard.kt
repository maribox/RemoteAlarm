package it.bosler.remotealarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.remotealarm.model.Alarms.Alarm
import it.bosler.remotealarm.model.Alarms.Days
import it.bosler.remotealarm.model.Alarms.Schedule
import it.bosler.remotealarm.viewmodel.AlarmViewModel
import java.time.LocalTime


@Composable
fun AlarmCard(alarm: Alarm, toggleAlarm: (Boolean) -> Unit) {
    // Row with rounded corners
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // onclick: open time picker


        //Text(alarm.time)
        // print all days in alarm.days
        if (alarm.schedule is Schedule.WeekdaysWithLocalTime) {
            Text(
                alarm.schedule.time.toString(),
                style = TextStyle(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.W400
                ),
                color = MaterialTheme.colorScheme.primary

            )
            for (day in alarm.schedule.days) {
                Text(day.value.asString())
            }
        }
        Switch(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(20))
                .clickable { toggleAlarm(!alarm.enabled) }
                .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            checked = alarm.enabled,
            onCheckedChange = toggleAlarm,
        )
    }
}


@Composable
fun AlarmCardList(
    alarms: List<Alarm>,
    onToggleAlarm: (Alarm) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(alarms) { alarm ->
            AlarmCard(alarm) { onToggleAlarm(alarm) }
        }
    }
}


@Preview
@Composable
fun AlarmCardPreview() {
    var enabled by remember { mutableStateOf(true) }
    AlarmCard(
        Alarm(
            0,
            schedule = Schedule.WeekdaysWithLocalTime(
                listOf(Days.MONDAY, Days.FRIDAY),
                LocalTime.MIDNIGHT
            ),
            enabled = enabled
        ),
    ) { enabled = !enabled; println("Toggled enabled to $enabled") }
}

/*
@Preview
@Composable
fun AlarmCardListPreview() {
    val alarmList = remember { AlarmViewModel.getAlarmList().toMutableStateList() }
    AlarmCardList(alarmList, onToggleAlarm = { alarm ->
        alarmList[alarm].enabled = !alarmList[alarm].enabled;
    })
}*/
