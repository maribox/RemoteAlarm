package it.bosler.remotealarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.remotealarm.R
import it.bosler.remotealarm.data.Alarms.Alarm
import it.bosler.remotealarm.data.Alarms.Days
import it.bosler.remotealarm.data.Alarms.Schedule
import it.bosler.remotealarm.shared.formatTwoDigits
import java.time.ZonedDateTime
import java.util.Locale


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
        // print all days in alarm.days
        Row(modifier = Modifier.height(80.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(.8f)) {
                if (alarm.schedule is Schedule.WeekdaysWithLocalTime) {
                    Text(
                        alarm.schedule.time.toString(),
                        style = TextStyle(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.W400
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        alarm.schedule.days.joinToString(", ") {
                            weekdaysMapShort[it].toString()
                        },
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W400
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (alarm.schedule is Schedule.SpecificMoment) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatTwoDigits(alarm.schedule.time.hour)}:${formatTwoDigits(alarm.schedule.time.minute)}",
                            style = TextStyle(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.W400
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "${formatTwoDigits(alarm.schedule.time.dayOfMonth)}.${formatTwoDigits(alarm.schedule.time.monthValue)}.",
                            style = TextStyle(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.W400
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
            Switch(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20))
                    .clickable { toggleAlarm(!alarm.enabled) }
                    .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .weight(.2f),
                checked = alarm.enabled,
                onCheckedChange = toggleAlarm,
            )
        }
    }
}



val weekdaysMap = mapOf(
    Days.MONDAY to R.string.monday,
    Days.TUESDAY to R.string.tuesday,
    Days.WEDNESDAY to R.string.wednesday,
    Days.THURSDAY to R.string.thursday,
    Days.FRIDAY to R.string.friday,
    Days.SATURDAY to R.string.saturday,
    Days.SUNDAY to R.string.sunday
)

val weekdaysMapShort = mapOf(
    Days.MONDAY to R.string.monday_short,
    Days.TUESDAY to R.string.tuesday_short,
    Days.WEDNESDAY to R.string.wednesday_short,
    Days.THURSDAY to R.string.thursday_short,
    Days.FRIDAY to R.string.friday_short,
    Days.SATURDAY to R.string.saturday_short,
    Days.SUNDAY to R.string.sunday_short
)

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
            schedule = Schedule.SpecificMoment(ZonedDateTime.now()),
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
