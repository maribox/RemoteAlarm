package it.bosler.remotealarm.model.Alarms

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.time.LocalDate

@Serializable
sealed class Schedule {
    @Serializable
    @SerialName("SpecificTimestamp")
    data class SpecificTimestamp(@Serializable val UTCtimestamp: Long) : Schedule()
    @Serializable
    @SerialName("WeekdaysWithLocalTime")
    data class WeekdaysWithLocalTime(@Serializable val days: List<Days>, @Serializable(with=LocalTimeSerializer::class) val time: LocalTime) : Schedule()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalTime::class)
class LocalTimeSerializer : KSerializer<LocalTime> {
    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.toSecondOfDay().toString())
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.ofSecondOfDay(decoder.decodeString().toLong())
    }
}


class ScheduleConverter {


    @TypeConverter
    fun scheduleToString(schedule: Schedule?): String {
        //println("Encoding: ")
        //println(Json.encodeToString(schedule))
        return Json.encodeToString(schedule)
    }

    @TypeConverter
    fun stringToSchedule(data: String?): Schedule? {
        if (data.isNullOrEmpty()) {
            return null
        }
        //println("Decoding: ")
        //println(data)
        //println(Json.decodeFromString<Schedule>(data))
        return Json.decodeFromString<Schedule>(data)
    }
}


@Entity
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val schedule: Schedule? = null,
    var enabled: Boolean = true,
)

