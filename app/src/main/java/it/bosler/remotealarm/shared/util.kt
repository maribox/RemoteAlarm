package it.bosler.remotealarm.shared
import java.util.Locale

const val MAX_COLOR_TEMP = 6500
const val MIN_COLOR_TEMP = 2700

fun formatTwoDigits(numberToFormat: Int): String =
    String.format(Locale.getDefault(), "%02d", numberToFormat)
