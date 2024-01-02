package it.bosler.remotealarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bosler.remotealarm.model.Alarms.Alarm
import it.bosler.remotealarm.model.Alarms.AlarmDAO
import it.bosler.remotealarm.model.Alarms.Days
import it.bosler.remotealarm.model.Alarms.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

class AlarmViewModel(
    private val dao: AlarmDAO
) : ViewModel() {

    // alarms should be a flow from the database, combining getAlarmsEnabled and getAlarmsDisabled
    private val _alarms = MutableStateFlow(listOf<Alarm>()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private val _state = MutableStateFlow(AlarmState())
    val state = combine(_state, _alarms) { _state, _alarms ->
        _state.copy(alarms = _alarms)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmState())

    fun onEvent(event: AlarmEvent) {
        when (event) {
            is AlarmEvent.ToggleEnabled -> {
                _state.value = _state.value.copy(currentAlarm = _state.value.currentAlarm.copy(enabled = !event.alarm.enabled))
            }
            is AlarmEvent.DeleteAlarm -> {
                viewModelScope.launch {
                    dao.deleteAlarm(event.alarm)
                }
            }
            is AlarmEvent.SetSchedule -> {
                _state.value = _state.value.copy(currentAlarm = _state.value.currentAlarm.copy(schedule = event.schedule))
            }
            AlarmEvent.SaveAlarm -> {
                println("Saving alarm ${state.value.currentAlarm}")
                if (state.value.currentAlarm.schedule == null) {
                    return
                }
                viewModelScope.launch {
                    dao.upsertAlarm(state.value.currentAlarm)
                    // TODO: try to look for a better solution here:
                    _state.update { it.copy(alarms = dao.getAlarmsEnabled().first()) }
                    println("Saved alarm ${_state.value.alarms.firstOrNull()}")
                    println("Saved alarm ${state.value.alarms.firstOrNull()}")

                }
                _state.update { it.copy(isEditingAlarm = false, currentAlarm = Alarm()) }
            }
            AlarmEvent.ShowDialog -> {
                _state.value = _state.value.copy(isEditingAlarm = true)
            }
            AlarmEvent.HideDialog -> {
                _state.value = _state.value.copy(isEditingAlarm = false)
            }
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        TODO()
        /*_alarms = _alarms.map {
            if (it.id == alarm.id) {
                it.copy(enabled = !it.enabled)
            } else {
                it
            }
        }*/
    }


    companion object {
        fun getAlarmList(): List<Alarm> {
            val list = listOf(
                Alarm(
                    0,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.NOON
                    ),
                    enabled = false
                ),
                Alarm(
                    1,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(11, 25)
                    ),
                    enabled = true
                ),
                Alarm(
                    2,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
                Alarm(
                    3,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
                Alarm(
                    4,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = false
                ),
                Alarm(
                    5,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
                Alarm(
                    6,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
                Alarm(
                    7,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
                Alarm(
                    8,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
                Alarm(
                    9,
                    schedule = Schedule.WeekdaysWithLocalTime(
                        listOf(Days.MONDAY, Days.FRIDAY),
                        LocalTime.of(12, 34)
                    ),
                    enabled = true
                ),
            )
            return list
        }
    }
}

