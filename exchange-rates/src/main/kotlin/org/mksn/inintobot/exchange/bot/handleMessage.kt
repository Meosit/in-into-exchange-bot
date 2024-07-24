package org.mksn.inintobot.exchange.bot

import io.ktor.client.plugins.*
import io.ktor.http.*
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.user.UserAggregateStats
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.bot.settings.Setting
import org.mksn.inintobot.exchange.output.BotOutputWithMessage
import org.mksn.inintobot.exchange.output.BotQuerySuccessOutput
import org.mksn.inintobot.exchange.output.BotSimpleErrorOutput
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.telegram.Message
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.logging.Logger


private val logger = Logger.getLogger("handleMessage")
private val repeatCommands = setOf("same", "this", "repeat", "повтор", "повтори", "это")

suspend fun Message.handle(
    settings: UserSettings,
    context: BotContext,
) {
    when (text) {
        "", null -> {
            logger.info("'$text' message text received")
            val errorMessages = BotMessages.errors.of(settings.language)
            context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(errorMessages.queryExpected))
            context.statsStore.logExchangeErrorRequest("queryExpected", inlineRequest = false)
        }
        "/start" -> {
            logger.info("Handling $text command")
            Setting.START_COMMAND.handle(null, this, settings, context)
            context.statsStore.logBotCommandUsage(text)
        }
        "/settings", "/start customise_settings" -> {
            logger.info("Handling $text command")
            Setting.ROOT.handle(null, this, settings, context)
            context.statsStore.logBotCommandUsage(text.replace(" customise_settings", "_set"))
        }
        "/stop", "/delete" -> {
            logger.info("Handling $text command")
            runCatching { context.settingsStore.delete(chat.id.toString()) }
                .onFailure {
                    val errorMessages = BotMessages.errors.of(settings.language)
                    context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(errorMessages.unableToSave))
                    context.statsStore.logExchangeErrorRequest("unableToSave", inlineRequest = false)
                }
                .onSuccess {
                    val stop = BotMessages.stopCommand.of(settings.language)
                    runCatching { context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(stop)) }
                        .exceptionOrNull()
                        ?.takeIf { it is ClientRequestException && it.response.status != HttpStatusCode.Forbidden }
                        ?.let { throw it }
                    context.statsStore.logSettingsChange(settings, null)
                    logger.info("Successfully deleted settings of used ${chat.id}")
                }
                .getOrThrow()
            context.statsStore.logBotCommandUsage(text)
        }
        "/help", "/patterns", "/apis" -> {
            logger.info("Handling $text command")
            val displayNames = BotMessages.apiDisplayNames.of(settings.language)
            val message =   when (text) {
                "/patterns" -> BotMessages.patternsCommand.of(settings.language)
                    .replace("{currencies}", Currencies
                        .joinToString("\n") { "- `${it.code}`:\n" + it.aliases.joinToString() })
                "/apis" -> {
                    val apisContent = RateApis.joinToString("\n\n") { rateApi ->
                        val displayName = displayNames.getValue(rateApi.name)
                        BotMessages.apiCommand.of(settings.language)
                            .replace("{name}", displayName)
                            .replace("{link}", rateApi.displayLink)
                            .replace("{base}", "`${rateApi.base.code}`")
                            .replace("{aliases}", (listOf(rateApi.name) + rateApi.aliases).joinToString { "`$it`" })
                            .replace("{unsupported}", rateApi.unsupported.ifEmpty { setOf("-/-") }.joinToString { "`$it`" })
                    }
                    BotMessages.apisCommand.of(settings.language).replace("{apis}", apisContent)
                }
                else -> BotMessages.helpCommand.of(settings.language)
                    .replace("{currency_count}", Currencies.size.toString())
                    .replace("{currency_list}", Currencies.joinToString { "`${it.code}`" })
                    .replace("{apis}", RateApis.joinToString { "[${displayNames.getValue(it.name)}](${it.displayLink})" })
            }
            val formattedMessage = BotTextOutput(message)
            context.sender.sendChatMessage(chat.id.toString(), formattedMessage)
            context.statsStore.logBotCommandUsage(text)
        }
        "/apistatus" -> {
            logger.info("Handling $text command")
            val statusFormat = BotMessages.apiStatusCommand.of(settings.language)
            val apiDisplayNames = BotMessages.apiDisplayNames.of(settings.language)
            val timeUnitNames = BotMessages.timeUnitNames.of(settings.language)
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val message = RateApis.joinToString(separator = "\n\n") { api ->
                val ratesUpdatedString = context.rateStore.getLatest(api.name)
                    ?.let { timeUnitNames.encodeToStringDuration(ZonedDateTime.of(it.date, it.time, ZoneOffset.UTC), now) }
                    ?: "∞"
                val ratesFrom = context.rateStore.historyStart(api.name)
                statusFormat.format(apiDisplayNames.getValue(api.name), ratesUpdatedString, ratesFrom.toString())
            }
            val formattedMessage = BotTextOutput(message)
            context.sender.sendChatMessage(chat.id.toString(), formattedMessage)
            context.statsStore.logBotCommandUsage(text)
        }
        "/donate" -> {
            logger.info("Handling $text command")
            context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(BotMessages.donateCommand.of(settings.language)))
            context.statsStore.logBotCommandUsage(text)
        }
        else -> {
            when {
                replyToMessage != null && text.lowercase() in repeatCommands -> {
                    logger.info("Handling repeat '$text' request")
                    replyToMessage.handle(settings, context)
                }
                text.startsWith("/stats") && this.chat.id.toString() == context.creatorId -> {
                    val stats = context.statsStore.get()
                    logger.info(stats.toString())
                    val markdown = makeStatsMessageMarkdown(stats)
                    context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(markdown))
                    context.statsStore.logBotCommandUsage("/stats")
                }
                text.startsWith("/myhourlyrate") && this.chat.id.toString() == context.creatorId -> {
                    logger.info("Handling '$text' command")
                    val query = text.removePrefix("/myhourlyrate").trim()
                    val output = handleBotExchangeQuery(isInline = false, query, settings.copy(outputCurrencies = listOf("USD")), context).first()
                    when {
                        output is BotQuerySuccessOutput || (output is BotOutputWithMessage && output.botOutput is BotQuerySuccessOutput) -> {
                            val unwrapped = (if (output is BotOutputWithMessage) output.botOutput else output) as BotQuerySuccessOutput
                            val usdExchange = unwrapped.exchanges.firstOrNull { it.currency.code == "USD" }
                            if (usdExchange != null) {
                                val hourlyRateUSD = usdExchange.value
                                context.settingsStore.save(chat.id.toString(), settings.copy(hourlyRateUSD = hourlyRateUSD))
                                context.sender.sendChatMessage(chat.id.toString(), unwrapped.copy(exchanges = listOf(usdExchange)))
                            } else {
                                val errorMessages = BotMessages.errors.of(settings.language)
                                context.sender.sendChatMessage(chat.id.toString(), BotSimpleErrorOutput(errorMessages.invalidMyHourlyRateUSD))
                                context.statsStore.logExchangeErrorRequest("unableToSaveHourlyRate", inlineRequest = false)
                            }
                        }
                        output is BotSimpleErrorOutput -> {
                            val errorMessages = BotMessages.errors.of(settings.language)
                            context.sender.sendChatMessage(chat.id.toString(), BotSimpleErrorOutput(errorMessages.invalidMyHourlyRateUSD))
                            context.statsStore.logExchangeErrorRequest("unableToSaveHourlyRate", inlineRequest = false)
                        }
                        else -> {
                            val errorMessages = BotMessages.errors.of(settings.language)
                            context.sender.sendChatMessage(chat.id.toString(), BotSimpleErrorOutput(errorMessages.invalidMyHourlyRateUSD))
                            context.statsStore.logExchangeErrorRequest("unableToSaveHourlyRate", inlineRequest = false)
                        }
                    }
                    context.statsStore.logBotCommandUsage("/myhourlyrate")
                }
                else -> {
                    logger.info("Handling '$text' chat message")
                    if (context.botUsername in (viaBot?.username ?: "")) {
                        logger.info("Bot inline query output used as chat input")
                        val messages = BotMessages.errors.of(settings.language)
                        context.sender.sendChatMessage(chat.id.toString(), BotSimpleErrorOutput(messages.inlineOutputAsChatInput))
                        context.statsStore.logExchangeErrorRequest("inlineOutputAsChatInput", inlineRequest = false)
                    } else {
                        val outputs = handleBotExchangeQuery(isInline = false, text, settings, context)
                        outputs.firstOrNull()?.let { context.sender.sendChatMessage(chat.id.toString(), it) }
                    }
                }
            }
        }
    }
}

private fun makeStatsMessageMarkdown(stats: UserAggregateStats) = """
*Total*: `${stats.totalRequests}` (chat: `${stats.totalRequests - stats.inlineRequests}`; inline: `${stats.inlineRequests}`)
*Errors*: `${stats.totalRequestsErrors}` (chat: `${stats.totalRequestsErrors - stats.inlineRequestsErrors}`; inline: `${stats.inlineRequestsErrors}`)
*With History*: `${stats.totalRequestsWithHistory}` (chat: `${stats.totalRequestsWithHistory - stats.inlineRequestsWithHistory}`; inline: `${stats.inlineRequestsWithHistory}`)
*Errors*:
${stats.errorUsage.toListString()}
*Commands*:
${stats.botCommandUsage.toListString(2)}
*Expressions*:
${
    stats.expressionTypeUsage.toListString(5) {
        when (it) {
            ExpressionType.ONE_UNIT -> "OU"
            ExpressionType.SINGLE_VALUE -> "SV"
            ExpressionType.SINGLE_CURRENCY_EXPR -> "SE"
            ExpressionType.MULTI_CURRENCY_EXPR -> "ME"
            ExpressionType.CURRENCY_DIVISION -> "CD"
            ExpressionType.CONVERSION_HISTORY -> "CH"
        }
    }
}
*Rate Apis*:
${stats.requestsRateApiUsage.toListString(4) { it.name.uppercase().take(3) }}
*Base Currency*:
${stats.requestsBaseCurrencyUsage.toListString(4, Currency::code)}
*Involved Currency*:
${stats.requestsInvolvedCurrencyUsage.toListString(4, Currency::code)}
*Output Currency*:
${stats.requestsOutputCurrencyUsage.toListString(4, Currency::code)}

*Settings*: `${stats.usersWithCustomizedSettings}`
${stats.settingsLanguageUsage.toListString(5)}
*Settings Default Currency*:
${stats.settingsDefaultCurrencyUsage.toListString(4, Currency::code)}
*Settings Default Api*:
${stats.settingsDefaultRateApiUsage.toListString(4) { it.name.uppercase().take(3) }}
*Settings Output*:
${stats.settingsOutputCurrencyUsage.toListString(4, Currency::code)}
                """.trim()

private fun <T> Map<T, Long>.toListString(entriesOnRow: Int = 1, selector: (T) -> String = { it.toString() }) = with(asSequence().sortedByDescending { it.value }) {
    if (entriesOnRow == 1)
        joinToString("\n") { (k, v) -> "`${v.toString().padStart(3)}` - ${selector(k)}" }
    else {
        val columnToMaxes = (0 until entriesOnRow).mapNotNull { columnIndex ->
            val columnVals = this.filterIndexed { i, _ -> i % entriesOnRow == columnIndex }
            if (columnVals.count() != 0) {
                val keyMax = columnVals.maxOf { (k, _) -> selector(k).length }
                val valMax = columnVals.maxOf { (_, v) -> v.toString().length }
                columnIndex to (keyMax to valMax)
            } else null
        }.toMap()
        chunked(entriesOnRow).joinToString("\n") { list ->
            list.mapIndexed { i, (k, v) ->
                val (keyMax, valMax) = columnToMaxes.getValue(i)
                "${selector(k).padEnd(keyMax)} ${v.toString().padEnd(valMax)}"
            }.joinToString("|", prefix = "`", postfix = "`")
        }
    }
}
