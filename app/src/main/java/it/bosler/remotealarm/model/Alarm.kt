package it.bosler.remotealarm.model


data class Alarm(
    val id: Int,
    val name: String,
    val time: String,
    val days: List<Days>,
)