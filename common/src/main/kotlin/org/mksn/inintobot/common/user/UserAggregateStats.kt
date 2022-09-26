package org.mksn.inintobot.common.user

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.rate.RateApi

data class UserAggregateStats(
    val totalRequests: Long,
    val totalRequestsWithHistory: Long,
    val inlineRequests: Long,
    val inlineRequestsWithHistory: Long,

    val totalRequestsErrors: Long,
    val inlineRequestsErrors: Long,
    val errorUsage: Map<String, Long>,

    val botCommandUsage: Map<String, Long>,


    val expressionTypeUsage: Map<ExpressionType, Long>,
    val requestsRateApiUsage: Map<RateApi, Long>,
    val requestsBaseCurrencyUsage: Map<Currency, Long>,
    val requestsInvolvedCurrencyUsage: Map<Currency, Long>,
    val requestsOutputCurrencyUsage: Map<Currency, Long>,

    val usersWithCustomizedSettings: Long,
    val settingsDefaultCurrencyUsage: Map<Currency, Long>,
    val settingsDefaultRateApiUsage: Map<RateApi, Long>,
    val settingsOutputCurrencyUsage: Map<Currency, Long>,
    val settingsLanguageUsage: Map<String, Long>,
)
