package it.bosler.remotealarm.viewmodel

import it.bosler.remotealarm.model.Alarms.Alarm

data class AlarmState(
    val alarms: List<Alarm> = emptyList(),
    val isEditingAlarm: Boolean = false,
    val currentAlarm: Alarm = Alarm()
){}