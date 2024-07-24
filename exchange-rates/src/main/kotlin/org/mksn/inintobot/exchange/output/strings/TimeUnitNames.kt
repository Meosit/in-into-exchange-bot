package org.mksn.inintobot.exchange.output.strings

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Serializable
data class TimeUnitNames(
    val dayShort: String,
    val dayOne: String,
    val dayTwoTillFour: String,
    val dayFiveTillTen: String,
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

    private fun nameOfDays(value: Long) = when (value % 100) {
        in 11L..20L -> dayFiveTillTen
        else -> when (value % 10) {
            1L -> dayOne
            in 2L..4L -> dayTwoTillFour
            else -> dayFiveTillTen
        }
    }

    fun encodeToStringDuration(time: ZonedDateTime, now: ZonedDateTime): String {
        val seconds = ChronoUnit.SECONDS.between(time, now)
        return encodeToStringDuration(seconds)
    }

    fun encodeToStringDuration(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            days != 0L && hours == 0L && minutes == 0L -> "`$days` ${nameOfDays(days)}"
            days == 0L && hours != 0L && minutes == 0L -> "`$hours` ${nameOfHours(hours)}"
            days == 0L && hours == 0L && minutes != 0L -> "`$minutes` ${nameOfMinutes(minutes)}"

            days != 0L && hours == 0L && minutes != 0L -> "`$days`${dayShort} `$minutes`${minuteShort}"
            days != 0L && hours != 0L && minutes == 0L -> "`$days`${dayShort} `$hours`${hourShort}"

            days == 0L && hours != 0L && minutes != 0L -> "`$hours`${hourShort} `$minutes`${minuteShort}"

            else -> "`$days`${dayShort} `$hours`${hourShort} `$minutes`${minuteShort}"
        }
    }

}