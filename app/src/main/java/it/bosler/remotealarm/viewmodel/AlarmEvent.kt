package it.bosler.remotealarm.viewmodel

import it.bosler.remotealarm.model.Alarms.Alarm
import it.bosler.remotealarm.model.Alarms.Schedule

sealed interface AlarmEvent {
    object SaveAlarm: AlarmEvent
    data class SetSchedule(val schedule: Schedule): AlarmEvent
    data class ToggleEnabled(val alarm: Alarm): AlarmEvent
    object ShowDialog: AlarmEvent
    object HideDialog: AlarmEvent
    data class DeleteAlarm(val alarm: Alarm): AlarmEvent
}
