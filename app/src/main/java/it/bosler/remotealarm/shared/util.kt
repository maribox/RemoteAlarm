package it.bosler.remotealarm.shared
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

const val MAX_COLOR_TEMP = 6500
const val MIN_COLOR_TEMP = 2700

fun formatTwoDigits(numberToFormat: Int): String =
    String.format(Locale.getDefault(), "%02d", numberToFormat)

fun Long.toBytes(): ByteArray {
    return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()
}

fun UShort.toBytes(): ByteArray {
    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(this.toShort()).array()
}