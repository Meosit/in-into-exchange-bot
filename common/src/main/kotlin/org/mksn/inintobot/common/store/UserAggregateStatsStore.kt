package org.mksn.inintobot.common.store

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.EvaluatedExpression
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.user.UserAggregateStats
import org.mksn.inintobot.common.user.UserSettings

interface UserAggregateStatsStore {

    fun get(): UserAggregateStats

    fun logExchangeRequestUsage(
        expression: EvaluatedExpression,
        rateApi: RateApi,
        outputCurrencies: List<Currency>,
        inlineRequest: Boolean,
        historyRequest: Boolean,
        customApi: Boolean
    ): Result<Unit>

    fun logExchangeErrorRequest(errorType: String, inlineRequest: Boolean): Result<Unit>

    fun logSettingsChange(old: UserSettings, new: UserSettings?): Result<Unit>

    fun logBotCommandUsage(command: String): Result<Unit>

    fun logMigratedTotalUsage(requests: Long, inlineRequests: Long): Result<Unit>

}