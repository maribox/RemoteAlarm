package it.bosler.remotealarm.model

import androidx.core.text.util.LocalePreferences.FirstDayOfWeek.Days

data class Alarm(
    val id: Int,
    val name: String,
    val time: String,
    val days: List<Days>,

)
