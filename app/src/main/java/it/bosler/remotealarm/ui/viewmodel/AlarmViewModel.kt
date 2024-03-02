package it.bosler.remotealarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import it.bosler.remotealarm.model.Alarms.Alarm
import it.bosler.remotealarm.model.Alarms.AlarmDAO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

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

    fun openNewAlarm() {
        _state.value =  _state.value.copy(isAlarmCreationOpen = true)
        _state.value =  _state.value.copy(currentEditedAlarm = Alarm(alarms.size))
    }

    fun closeCurrentAlarm() {
        // TODO: Save to database
        _state.value =  _state.value.copy(isAlarmCreationOpen = false)
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

