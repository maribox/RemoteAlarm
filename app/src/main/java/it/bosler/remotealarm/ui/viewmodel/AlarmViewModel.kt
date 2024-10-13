package it.bosler.remotealarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import it.bosler.remotealarm.data.Alarms.Alarm
import it.bosler.remotealarm.data.Alarms.Schedule
import it.bosler.remotealarm.data.Alarms.ScheduleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

class AlarmViewModel (
    //private val dao: AlarmDAO
) : ViewModel() {
    private val _state = MutableStateFlow(AlarmsScreenState());
    val state : StateFlow<AlarmsScreenState> = _state.asStateFlow()

    var alarms : MutableList<Alarm> = mutableListOf();

    // Events
    fun toggleAlarm(alarm: Alarm) {
        println(alarms)
        val index = alarms.indexOfFirst { it == alarm }
        if (index != -1) {
            alarms[index] = alarms[index].copy(enabled = !alarms[index].enabled)
        }
        _state.value = _state.value.copy(alarms = alarms.toList())
    }

    fun changeCurrentAlarmType(scheduleType: ScheduleType) {
        val newSchedule = when(scheduleType) {
            ScheduleType.SpecificTimestamp -> Schedule.SpecificTimestamp(0)
            ScheduleType.WeekdaysWithLocalTime -> Schedule.WeekdaysWithLocalTime(listOf(), LocalTime.now())
        }
        _state.value = _state.value.copy(currentEditedAlarm = _state.value.currentEditedAlarm!!.copy(schedule = newSchedule))
    }

    fun openNewAlarm() {
        _state.value =  _state.value.copy(isAlarmEditOpen = true)
        _state.value =  _state.value.copy(currentEditedAlarm = Alarm(alarms.size, schedule = Schedule.SpecificTimestamp(0)))
    }

    fun closeCurrentAlarm() {
        // TODO: Save to database
        _state.value =  _state.value.copy(isAlarmEditOpen = false)
        _state.value =  _state.value.copy(currentEditedAlarm = Alarm())
    }

    fun saveCurrentAlarm() {
        alarms.add(_state.value.currentEditedAlarm!!)
        _state.value = _state.value.copy(alarms = alarms.toList())
        closeCurrentAlarm()
    }

    fun enableCurrentAlarm(checked: Boolean) {
        _state.value = _state.value.copy(currentEditedAlarm = _state.value.currentEditedAlarm!!.copy(enabled = checked))
    }

}

data class AlarmsScreenState(
    val alarms: List<Alarm> = emptyList(),
    val isAlarmEditOpen: Boolean = false,
    val currentEditedAlarm: Alarm? = null,
)