package it.bosler.remotealarm.ui.viewmodel

import it.bosler.remotealarm.model.Alarms.Alarm

data class AlarmsScreenState(
    val alarms: List<Alarm> = emptyList(),
    val isAlarmCreationOpen: Boolean = false,
    val currentEditedAlarm: Alarm? = null,
)