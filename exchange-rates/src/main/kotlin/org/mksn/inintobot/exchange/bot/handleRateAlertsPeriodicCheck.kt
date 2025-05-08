package org.mksn.inintobot.exchange.bot

import kotlinx.coroutines.delay
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import org.mksn.inintobot.common.user.RateAlert
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.output.BotQueryHistoryOutput
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.HistoryConversion
import org.mksn.inintobot.exchange.output.strings.BotMessages
import java.time.LocalDate
import java.util.logging.Logger
import kotlin.math.min


private val logger = Logger.getLogger("handleRateAlertsPeriodicCheck")


suspend fun handleRateAlertsPeriodicCheck(context: BotContext) {
    logger.info("Handling Rate Alerts Check...")
    val settingsWithAlerts = context.settingsStore.getAllWithAlerts()
    settingsWithAlerts
        .also { logger.info("Found ${it.size} users with configured alerts") }
        .flatMap { (userId, settings) ->
            settings.alerts?.map { alert -> InvertedRateAlert(userId, alert) } ?: emptyList()
        }
        .also { logger.info("Total ${it.size} alerts") }
        .groupBy { it.alert.apiName }
        .also { logger.info("Querying ${it.size} apis: ${it.keys.joinToString()}") }
        .map { (apiName, invertedAlerts) -> context.rateStore.processAlertsForApi(apiName, invertedAlerts) }
        .asSequence()
        .flatten()
        .groupBy { it.userId }
        .mapValues { (userId, processedAlerts) ->
            val settings = settingsWithAlerts.getValue(userId)
            val apiDisplayNames = BotMessages.apiDisplayNames.of(settings.language)
            processedAlerts
                .map {
                    BotQueryHistoryOutput(
                        it.currencies, settings.language, it.conversions, min(settings.decimalDigits, 4),
                        apiDisplayNames.getValue(it.alert.apiName), it.apiTime,
                        rateAlert = it.alert
                    ).markdown()
                }
                .joinToString("\n\n————\n\n") { it.trim() }
                .let { BotTextOutput(it) }
        }.also { logger.info("${it.size} users needs to be notified") }
        .forEach { (userId, output) ->
            context.sender.sendChatMessage(userId, output)
            logger.info("Sent alert to user $userId")
            delay(1000)
        }
}


data class InvertedRateAlert(
    val userId: String,
    val alert: RateAlert,
)

data class ProcessedRateAlert(
    val alert: RateAlert,
    val userId: String,
    val apiTime: String,
    val currencies: List<Currency>, // always 2
    val conversions: List<HistoryConversion>, // always 2
)


private fun ApiExchangeRateStore.processAlertsForApi(
    apiName: String,
    alertsForApi: List<InvertedRateAlert>,
): List<ProcessedRateAlert> {
    val todayRates = this.getForDate(apiName, LocalDate.now(), backtrackDays = 0)
        ?: return emptyList()
    val yesterdayRates = this.getForDate(apiName, LocalDate.now().minusDays(1), backtrackDays = 0)
        ?: return emptyList()
    val apiTime = todayRates.timeString()

    return alertsForApi.mapNotNull { invertedRateAlert ->
        val alert = invertedRateAlert.alert
        val source = Currencies[alert.fromCurrency]
        val target = Currencies[alert.toCurrency]
        val todayValue = todayRates.exchange(1.toFixedScaleBigDecimal(), source, target)
        val yesterdayValue = yesterdayRates.exchange(1.toFixedScaleBigDecimal(), source, target)

        if (
            (alert.isRelative && (todayValue - yesterdayValue).abs() >= alert.value)
            || (!alert.isRelative && (alert.value in yesterdayValue..todayValue || alert.value in todayValue..yesterdayValue))
        ) {
            ProcessedRateAlert(
                alert = alert,
                userId = invertedRateAlert.userId,
                apiTime = apiTime,
                currencies = listOf(source, target),
                conversions = listOf(
                    HistoryConversion(
                        date = todayRates.date,
                        current = todayValue,
                        previous = yesterdayValue
                    ),
                    HistoryConversion(
                        date = yesterdayRates.date,
                        current = yesterdayValue,
                        previous = null
                    )
                )
            )
        } else null
    }
}