package it.bosler.remotealarm.ui.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import it.bosler.remotealarm.bluetooth.BluetoothManager
import it.bosler.remotealarm.data.Alarms.Alarm
import it.bosler.remotealarm.data.Alarms.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZonedDateTime

class AlarmViewModel(
    //private val dao: AlarmDAO
    bluetoothManager: BluetoothManager
) : ViewModel() {

    companion object {
        fun get_factory(bluetoothManager: BluetoothManager) = object : ViewModelProvider.Factory
        {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AlarmViewModel(bluetoothManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    private val _state = MutableStateFlow(AlarmsScreenState())
    val state: StateFlow<AlarmsScreenState> = _state.asStateFlow()

    var alarms: MutableList<Alarm> = mutableListOf()

    // Events
    fun toggleAlarm(alarm: Alarm) {
        val updatedAlarms = _state.value.alarms.map {
            if (it == alarm) it.copy(enabled = !it.enabled) else it
        }
        _state.value = _state.value.copy(alarms = updatedAlarms)
    }

    /*fun changeCurrentAlarmType(scheduleType: ScheduleType) {
        val newSchedule = when (scheduleType) {
            ScheduleType.SpecificTimestamp -> Schedule.SpecificMoment(Instant.now())
            ScheduleType.WeekdaysWithLocalTime -> Schedule.WeekdaysWithLocalTime(listOf(), LocalTime.now())
        }
        _state.value = _state.value.copy(
            currentEditedAlarm = _state.value.currentEditedAlarm!!.copy(schedule = newSchedule)
        )
    }*/

    fun openNewAlarm() {
        _state.value = _state.value.copy(isAlarmEditOpen = true)

        val dateTimeIn8h = ZonedDateTime.now().plusHours(8)
        val dateUTCPlusOffsetMillis = (dateTimeIn8h.toEpochSecond() + dateTimeIn8h.offset.totalSeconds)*1000
        changeCurrentAlarmTime(dateTimeIn8h.hour, dateTimeIn8h.minute)
        changeCurrentAlarmDateUTC(dateUTCPlusOffsetMillis)
    }

    fun closeCurrentAlarm() {
        _state.value = _state.value.copy(isAlarmEditOpen = false)
    }

    fun saveCurrentAlarm() {
        val updatedAlarms = _state.value.alarms.toMutableList()
        val now = ZonedDateTime.now()
        val moment = ZonedDateTime.ofInstant(Instant.ofEpochMilli(_state.value.dateStartUTC + now.offset.totalSeconds), now.zone)
            .withHour(_state.value.hour)
            .withMinute(_state.value.minute)

        if (moment.isBefore(now)) {
            //toast that the alarm is in the past
            _state.value = _state.value.copy(errorMessage = "Alarm is in the past")
        } else {
            updatedAlarms.add(
                Alarm(schedule = Schedule.SpecificMoment(moment))
            )
            _state.value = _state.value.copy(alarms = updatedAlarms)
            closeCurrentAlarm()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun changeCurrentAlarmDateUTC(dateStartUTC: Long) {
        _state.value = _state.value.copy(dateStartUTC=dateStartUTC)
    }

    fun changeCurrentAlarmTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour=hour, minute=minute)
    }
}

data class AlarmsScreenState(
    val alarms: List<Alarm> = emptyList(),
    val isAlarmEditOpen: Boolean = false,
    val dateStartUTC: Long = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val errorMessage : String? = null
)