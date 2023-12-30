package it.bosler.remotealarm.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate


sealed class Schedule {
    data class SpecificDate(val date: LocalDate) : Schedule()
    data class Weekdays(val days: List<Days>) : Schedule()
}
data class Alarm(
    val id: Int,
    val name: String,
    val time: String,
    val schedule: Schedule,
    var initialEnabled: Boolean = true,
) {
    // TODO: Temporary solution for debugging. Implement ViewModel later
    var enabled : Boolean by mutableStateOf(initialEnabled)
}