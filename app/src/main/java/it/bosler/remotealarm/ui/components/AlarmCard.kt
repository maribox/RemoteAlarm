package it.bosler.remotealarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import it.bosler.remotealarm.model.Alarm
import it.bosler.remotealarm.model.Days


@Composable
fun AlarmCard(alarm: Alarm, toggleAlarm: (Boolean) -> Unit) {
    // Row with rounded corners
    Row (
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        // onclick: open time picker
        Text(alarm.time,
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.W400
            ),
            color = MaterialTheme.colorScheme.primary

        )

        //Text(alarm.time)
        // print all days in alarm.days
        for (day in alarm.days) {
            Text(day.value.asString())
        }
        Switch(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(20))
                .clickable { toggleAlarm(!alarm.enabled) }
                .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ,
            checked = alarm.enabled,
            onCheckedChange = toggleAlarm,
        )
    }
}

private fun getAlarmList(): List<Alarm> {
    val list = listOf(
        Alarm(
            0,
            "Alarm 1",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = false
        ),
        Alarm(
            1,
            "Alarm 2",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            2,
            "Alarm 3",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            3,
            "Alarm 4",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            4,
            "Alarm 5",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = false
        ),
        Alarm(
            5,
            "Alarm 6",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            6,
            "Alarm 7",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            7,
            "Alarm 8",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            8,
            "Alarm 9",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
        Alarm(
            9,
            "Alarm 10",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = true
        ),
    )
    return list
}

@Composable
fun AlarmCardList(
    alarms: List<Alarm>,
    onDisableAlarm: (Int) -> Unit
) {
    LazyColumn (modifier = Modifier.padding(8.dp)){
        itemsIndexed(items = alarms) { index, alarm->
            AlarmCard(alarm) { onDisableAlarm(index) }
            Spacer(modifier = Modifier.padding(2.dp))
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
            "Alarm 1",
            "12:00",
            days = listOf(Days.MONDAY, Days.FRIDAY),
            initialEnabled = enabled
        ),
    ) { enabled = !enabled; println("Toggled enabled to $enabled") }
}

@Preview
@Composable
fun AlarmCardListPreview() {
    val alarmList = remember { getAlarmList().toMutableStateList() }
    AlarmCardList(alarmList, onDisableAlarm = { alarm ->
        alarmList[alarm].enabled = !alarmList[alarm].enabled;
    })
}