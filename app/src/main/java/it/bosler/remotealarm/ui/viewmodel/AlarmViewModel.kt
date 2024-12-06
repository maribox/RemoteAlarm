package it.bosler.remotealarm.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.juul.kable.State
import it.bosler.remotealarm.domain.BluetoothManager
import it.bosler.remotealarm.data.Alarms.Alarm
import it.bosler.remotealarm.data.Alarms.Schedule
import it.bosler.remotealarm.shared.toBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZonedDateTime
import java.time.Duration

class AlarmViewModel(
    //private val dao: AlarmDAO
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    companion object {
        fun getFactory(bluetoothManager: BluetoothManager) = object : ViewModelProvider.Factory
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
            .withSecond(0)

        if (moment.isBefore(now)) {
            //toast that the alarm is in the past
            _state.value = _state.value.copy(errorMessage = "Alarm is in the past")
        } else {
            // upload to device
            val alarm = Alarm(schedule = Schedule.SpecificMoment(moment), action = _state.value.alarmAction)
            if (bluetoothManager.connectionState?.value == State.Connected) {
                bluetoothManager.addAlarm(alarm)
                Log.d("AlarmViewModel", "Alarm added to device")
            }

            // update local state
            // TODO: fetch from remote
            updatedAlarms.add(alarm)
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

    fun changeCurrentAlarmAction(hasRamp: Boolean = _state.value.alarmAction.hasRamp,
                                 rampDuration: Duration = _state.value.alarmAction.rampDuration,
                                 targetDuration: Duration = _state.value.alarmAction.targetDuration,
                                 shouldBlink: Boolean = _state.value.alarmAction.shouldBlink,
                                 blinkInterval: Duration = _state.value.alarmAction.blinkPeriodLength,
                                 blinkDuration: Duration = _state.value.alarmAction.blinkDuration,
                                 targetIntensity: Double = _state.value.alarmAction.targetIntensity,
                                 colorTemperatureBalance: Double = _state.value.alarmAction.colorTemperatureBalance) {
        _state.value = _state.value.copy(alarmAction = AlarmAction(hasRamp = hasRamp, rampDuration = rampDuration,
            targetDuration = targetDuration, shouldBlink = shouldBlink, blinkPeriodLength = blinkInterval,
            blinkDuration = blinkDuration, targetIntensity = targetIntensity, colorTemperatureBalance = colorTemperatureBalance))
    }
}

data class AlarmsScreenState(
    val alarms: List<Alarm> = emptyList(),
    val isAlarmEditOpen: Boolean = false,

    val dateStartUTC: Long = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val alarmAction : AlarmAction = AlarmAction(),

    val errorMessage : String? = null
)

data class AlarmAction (
    val hasRamp: Boolean = true,
    val rampDuration: Duration = Duration.ofSeconds(30),

    val targetDuration: Duration = Duration.ofSeconds(30), // duration after which blinking should start or alarm should turn off
    val targetIntensity: Double = 0.0,
    val colorTemperatureBalance: Double = 0.0,

    val shouldBlink: Boolean = true,
    val blinkPeriodLength: Duration = Duration.ofSeconds(2),
    val blinkDuration: Duration = Duration.ofMinutes(10),
)

enum class LightProgramTypes(val value: Byte) {
    FIXED(0),
    RAMP(1),
    BLINK(2),
}

fun AlarmAction.toLightProgramBytes(): ByteArray {
    val byteList = mutableListOf<Byte>()

    if (hasRamp) {
        byteList.add(LightProgramTypes.RAMP.value)
        byteList.addAll(rampDuration.toMillis().toBytes().toList())
        val (cw, ww) = BluetoothManager.calculateLightStateBytes(targetIntensity, colorTemperatureBalance)
        byteList.addAll(listOf(cw, ww))
    }

    byteList.add(LightProgramTypes.FIXED.value)
    byteList.addAll(targetDuration.toMillis().toBytes().toList())
    val (cw, ww) = BluetoothManager.calculateLightStateBytes(targetIntensity, colorTemperatureBalance)
    byteList.addAll(listOf(cw, ww))

    if (shouldBlink) {
        byteList.add(LightProgramTypes.BLINK.value)
        byteList.addAll((blinkDuration.toMillis()).toBytes().toList())
        val highLowDurationBytes = (blinkPeriodLength.toMillis()/2).coerceIn(0, UShort.MAX_VALUE.toLong()).toUShort().toBytes().toList()
        byteList.addAll(highLowDurationBytes)
        byteList.addAll(highLowDurationBytes)
        val (cwHigh, wwHigh) = BluetoothManager.calculateLightStateBytes(targetIntensity, colorTemperatureBalance)
        val (cwLow, wwLow) = BluetoothManager.calculateLightStateBytes(0.0, 0.0)
        byteList.addAll(listOf(cwHigh, wwHigh, cwLow, wwLow))
    }
    return byteList.toByteArray()
}

