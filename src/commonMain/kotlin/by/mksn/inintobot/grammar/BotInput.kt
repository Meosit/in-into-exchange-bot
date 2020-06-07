package by.mksn.inintobot.grammar

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.expression.Expression

/**
 * Represents the successfully parsed input string
 *
 * @property expression input [Expression] which can be evaluated
 * @property additionalCurrencies a set of [Currency] which were additionally requested
 * @property rateApi requested API for evaluation
 */
data class BotInput(
    val expression: Expression,
    val additionalCurrencies: Set<Currency>,
    val rateApi: RateApi
)