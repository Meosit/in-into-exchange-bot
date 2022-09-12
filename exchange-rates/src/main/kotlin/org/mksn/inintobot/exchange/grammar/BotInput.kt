package org.mksn.inintobot.exchange.grammar

import org.mksn.inintobot.currency.Currency
import org.mksn.inintobot.exchange.expression.Expression
import org.mksn.inintobot.rates.RateApi

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
    val decimalDigits: Int?
)