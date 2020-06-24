package by.mksn.inintobot.misc

import kotlinx.serialization.Serializable

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
)