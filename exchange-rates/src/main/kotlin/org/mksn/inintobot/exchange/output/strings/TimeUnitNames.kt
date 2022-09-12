package org.mksn.inintobot.exchange.output.strings

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Serializable
data class TimeUnitNames(
    val hourShort: String,
    val hourOne: String,
    val hourTwoTillFour: String,
    val hourFiveTillTen: String,
    val minuteShort: String,
    val minuteOne: String,
    val minuteTwoTillFour: String,
    val minuteFiveTillTen: String
) {

    private fun nameOfMinutes(value: Long) = when (value % 100) {
        in 11L..20L -> minuteFiveTillTen
        else -> when (value % 10) {
            1L -> minuteOne
            in 2L..4L -> minuteTwoTillFour
            else -> minuteFiveTillTen
        }
    }

    private fun nameOfHours(value: Long) = when (value % 100) {
        in 11L..20L -> hourFiveTillTen
        else -> when (value % 10) {
            1L -> hourOne
            in 2L..4L -> hourTwoTillFour
            else -> hourFiveTillTen
        }
    }

    fun encodeToStringDuration(time: ZonedDateTime, now: ZonedDateTime): String {
        val hours = ChronoUnit.HOURS.between(time, now)
        val minutes = ChronoUnit.MINUTES.between(time, now) - hours * 60
        return when {
            hours == 0L && minutes != 0L -> "`$minutes` ${nameOfMinutes(minutes)}"
            hours != 0L && minutes == 0L -> "`$hours` ${nameOfHours(hours)}"
            else -> "`$hours`${hourShort} `$minutes`${minuteShort}"
        }
    }

}