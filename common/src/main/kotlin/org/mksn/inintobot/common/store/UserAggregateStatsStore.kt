package org.mksn.inintobot.common.store

import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.user.UserAggregateStats
import java.util.*

interface UserAggregateStatsStore {

    fun get(): UserAggregateStats

    fun logRequestUsage(
        errorRequest: Boolean,
        inlineRequest: Boolean,
        expressionRequest: Boolean,
        involvedCurrencies: List<Currency>,
        rateApi: RateApi,
    )

    fun logSettingsUsage()


}