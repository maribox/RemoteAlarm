package it.bosler.remotealarm.model.Alarms

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Alarm::class],
    version = 1
)
@TypeConverters(ScheduleConverter::class)
abstract class AlarmDatabase: RoomDatabase() {
    abstract val dao: AlarmDAO
}