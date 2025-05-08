package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.scaled
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.misc.trimToLength
import org.mksn.inintobot.common.user.RateAlert
import org.mksn.inintobot.exchange.output.strings.BotMessages
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class HistoryConversion(
    val date: LocalDate,
    val current: BigDecimal?,
    val previous: BigDecimal?
)

data class BotQueryHistoryOutput(
    val historyCurrencies: List<Currency>,
    val language: String,
    val conversions: List<HistoryConversion>,
    val decimalDigits: Int,
    val apiName: String,
    val apiTime: String,
    val rateAlert: RateAlert? = null,
) : BotOutput {
    private val noRate = "-.${"-".repeat(decimalDigits)}"
    private val strings = BotMessages.query.of(language)
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag(language))

    private val expressionHeader = if (rateAlert != null) {
        strings.headers.alert.format(*historyCurrencies.map { "${it.emoji}${it.code}" }.toTypedArray(),
            (if (rateAlert.isRelative) "±" else "⇵") + rateAlert.value.toStr(decimalDigits),
            historyCurrencies[1].let { "${it.emoji}${it.code}" },
        )
    } else
        strings.headers.history.format(*historyCurrencies.map { "${it.emoji}${it.code}" }.toTypedArray())

    private fun BigDecimal?.toSignedDiff(current: BigDecimal?) = this?.scaled(decimalDigits)
        ?.let { current?.scaled(decimalDigits)?.minus(it) }
        ?.let { (if (it > BigDecimal.ZERO) "+" else "") + it.toStr(decimalDigits, stripZeros = false, precise = false) }


    private fun LocalDate.dayName() = format(dayFormatter).replace(".", "").replaceFirstChar(Char::uppercaseChar)

    private val markdown = let {
        val apiHeader = apiName.let { strings.headers.api.format(it) }
        val apiTime = apiTime.let { (if (":" in it) strings.headers.apiTime else strings.headers.apiDate).format(it) }
        val longestDayName = conversions.maxOf { it.date.dayName().length }
        val longestRate = conversions.maxOf {
            it.current?.toStr(decimalDigits, stripZeros = false, precise = false)?.length ?: 0
        }
        val longestDiff = conversions.maxOf { it.previous.toSignedDiff(it.current)?.length ?: 0 }
        val exchangeBody = conversions.joinToString("\n") {
            val dayNumber = it.date.dayOfMonth.toString().padStart(2)
            val dayName = it.date.dayName().padEnd(longestDayName)
            val rate = (it.current?.toStr(decimalDigits, stripZeros = false, precise = false) ?: noRate).padStart(longestRate)
            val diff = (it.previous.toSignedDiff(it.current) ?: noRate).padStart(longestDiff)
            val emoji = when {
                diff.trim() == noRate -> "⏸"
                '+' in diff -> "\uD83D\uDCC8"
                '-' in diff -> "\uD83D\uDCC9"
                else -> "⏸"
            }
            "`$dayNumber $dayName`  `$rate`  `$diff`$emoji"
        }
        (expressionHeader + apiHeader + apiTime + exchangeBody).trimToLength(
            BotMessages.maxOutputLength,
            tail = "… ${strings.outputTooBigMessage}"
        )
    }

    override fun inlineTitle() = strings.inlineTitles.history
        .format(*historyCurrencies.map { "${it.emoji}${it.code}" }.toTypedArray())

    override fun inlineDescription() = markdown
        .replace("`", "")
        .replace("_", "")
        .replace("\n", " ")

    override fun inlineThumbUrl() = strings.inlineThumbs.history

    override fun markdown() = markdown
}