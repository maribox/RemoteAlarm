package it.bosler.remotealarm.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import it.bosler.remotealarm.model.Alarms.Schedule
import it.bosler.remotealarm.ui.components.AlarmCardList
import it.bosler.remotealarm.viewmodel.AlarmEvent
import it.bosler.remotealarm.viewmodel.AlarmState
import it.bosler.remotealarm.viewmodel.AlarmViewModel

@Composable
fun AlarmsScreen(
    state: AlarmState,
    onEvent: (AlarmEvent) -> Unit
) {
    onEvent(AlarmEvent.SetSchedule(Schedule.SpecificTimestamp(123)))
    onEvent(AlarmEvent.SaveAlarm)
    AlarmCardList(alarms = state.alarms, onToggleAlarm = { onEvent(AlarmEvent.ToggleEnabled(it))})
}