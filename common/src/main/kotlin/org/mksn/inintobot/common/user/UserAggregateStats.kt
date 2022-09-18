package org.mksn.inintobot.common.user

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.rate.RateApi

data class UserAggregateStats(
    val users: Long,
    val usersWithSettings: Long,
    val requests: Long,
    val errorRequests: Long,
    val inlineRequests: Long,
    val errorInlineRequests: Long,
    val expressionRequests: Long,
    val expressionInlineRequests: Long,
    val botCommandUsage: Map<String, Long>,
    val settingsCurrencyUsage: Map<Currency, Long>,
    val settingsRateApiUsage: Map<RateApi, Long>,
    val currencyUsage: Map<Currency, Long>,
    val rateApiUsage: Map<RateApi, Long>
)
