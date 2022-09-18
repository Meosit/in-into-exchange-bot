package org.mksn.inintobot.exchange.grammar

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.exchange.expression.Expression

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