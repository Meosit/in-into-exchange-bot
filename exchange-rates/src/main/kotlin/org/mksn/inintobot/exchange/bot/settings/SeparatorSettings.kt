package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.user.UserSettings

fun separatorsAreAmbiguous(thousandSeparator: Char?, decimalSeparator: Char): Boolean =
    thousandSeparator != null && thousandSeparator == decimalSeparator

fun String.toThousandSeparator(): Char? = if (this == "null") null else first()

fun UserSettings.exampleNumber(): String =
    "1234567.12".toFixedScaleBigDecimal().toStr(2, thousandSeparator = thousandSeparator, decimalSeparator = decimalSeparator)
