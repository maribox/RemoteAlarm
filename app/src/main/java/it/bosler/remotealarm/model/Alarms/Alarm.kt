package it.bosler.remotealarm.model.Alarms

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed class Schedule {
    data class SpecificTimestamp(val UTCtimestamp: Long) : Schedule()
    data class WeekdaysWithLocalTime(val days: List<Days>, val time: LocalTime) : Schedule()

    override fun toString(): String {
        return when (this) {
            is SpecificTimestamp -> {
                "SpecificTimestamp: $UTCtimestamp"
            }
            is WeekdaysWithLocalTime -> {
                "WeekdaysWithLocalTime: ${days.joinToString(", ")} at ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }
        }
    }
}

class ScheduleConverter {
    // TODO: Setup Serialization/Deserialization for Schedule with kotlin serialization
    private val gson = Gson()

    @TypeConverter
    fun scheduleToString(schedule: Schedule?): String {
        println(gson.toJson(schedule))
        return gson.toJson(schedule)
    }

    @TypeConverter
    fun stringToSchedule(data: String?): Schedule? {
        if (data.isNullOrEmpty()) {
            return null
        }
        val type = object : TypeToken<Schedule>() {}.type
        println(data)
        println(gson.fromJson(data, type) as Schedule)
        return gson.fromJson(data, type) as Schedule
    }
}


@Entity
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val schedule: Schedule? = null,
    var enabled: Boolean = true,
)

