package it.bosler.remotealarm.model.Alarms

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDAO {

    @Upsert
    suspend fun upsertAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("SELECT * FROM alarm ORDER BY ID ASC")
    fun getAlarmsOrderedById(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarm WHERE enabled == 1")
    fun getAlarmsEnabled(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarm WHERE enabled == 0")
    fun getAlarmsDisabled(): Flow<List<Alarm>>

}