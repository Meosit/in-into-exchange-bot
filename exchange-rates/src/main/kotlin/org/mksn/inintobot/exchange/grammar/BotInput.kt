package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.ErrorResult
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.Expression
import org.mksn.inintobot.common.rate.RateApi
import java.time.LocalDate

/**
 * Represents the successfully parsed input string
 *
 * @property expression input [Expression] which can be evaluated
 * @property additionalCurrencies a set of [Currency] which were additionally requested
 * @property rateApi requested API for evaluation
 * @property decimalDigits number of digits after decimal point
 */
data class BotInput(
    val expression: Expression,
    val additionalCurrencies: Set<Currency>,
    val rateApi: RateApi?,
    val onDate: LocalDate?,
    val decimalDigits: Int?,
    val historyView: Boolean = false
)
data class InvalidDate(val match: TokenMatch): ErrorResult()