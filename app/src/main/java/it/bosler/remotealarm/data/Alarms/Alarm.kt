package it.bosler.remotealarm.data.Alarms

import it.bosler.remotealarm.R
import java.time.LocalTime

sealed class Schedule {
    abstract val scheduleType: ScheduleType
    data class SpecificTimestamp(val UTCtimestamp: Long) : Schedule() {
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
    val schedule: Schedule = Schedule.SpecificTimestamp(0),
    var enabled: Boolean = true,
)

