package it.bosler.remotealarm.data.Alarms

import it.bosler.remotealarm.R
import it.bosler.remotealarm.ui.viewmodel.AlarmAction
import java.time.LocalTime
import java.time.ZonedDateTime

sealed class Schedule {
    abstract val scheduleType: ScheduleType
    data class SpecificMoment(val time: ZonedDateTime) : Schedule() {
        override val scheduleType = ScheduleType.SpecificTimestamp
    }
    data class WeekdaysWithLocalTime(val days: List<Days>, val time: LocalTime) : Schedule() {
        override val scheduleType = ScheduleType.WeekdaysWithLocalTime
    }
}

enum class ScheduleType (val uiNameResId: Int) {
    SpecificTimestamp(R.string.specific_timestamp_short),
    WeekdaysWithLocalTime(R.string.weekdays_short)
}

data class Alarm(
    val id: Int = 0,
    var enabled: Boolean = true,
    val schedule: Schedule = Schedule.SpecificMoment(ZonedDateTime.now()),
    val action : AlarmAction = AlarmAction(),
) {
    companion object
}

