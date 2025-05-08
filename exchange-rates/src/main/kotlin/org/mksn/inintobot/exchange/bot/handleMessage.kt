package org.mksn.inintobot.exchange.bot

import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import io.ktor.client.plugins.*
import io.ktor.http.*
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.Add
import org.mksn.inintobot.common.expression.Const
import org.mksn.inintobot.common.expression.ConversionHistoryExpression
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.user.RateAlert
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.user.UserAggregateStats
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.bot.settings.Setting
import org.mksn.inintobot.exchange.grammar.BotInputGrammar
import org.mksn.inintobot.exchange.output.*
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.telegram.Message
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
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
                    handleMyHourlyRateCommand(text, settings, context)
                }
                text.startsWith("/alert_delta") && this.chat.id.toString() == context.creatorId -> {
                    handleAlertRateCommand(settings, text, context, command = "/alert_delta")
                }
                text.startsWith("/alert_rate") && this.chat.id.toString() == context.creatorId -> {
                    handleAlertRateCommand(settings, text, context, command = "/alert_rate")
                }
                text.startsWith("/checkalerts") && this.chat.id.toString() == context.creatorId -> {
                    handleRateAlertsPeriodicCheck(context)
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

private suspend fun Message.handleAlertRateCommand(
    settings: UserSettings,
    text: String,
    context: BotContext,
    command: String
) {
    val defaultApi = RateApis[settings.apiName]

    val query = text.removePrefix(command).trim()
    val isRelative = "delta" in command

    when (val result = BotInputGrammar.tryParseRateAlertInput(query)) {
        is Parsed -> with(result.value) {
            val api = rateApi ?: defaultApi
            logger.info("Chosen api is ${api.name} (default: ${defaultApi.name})")
            if (expression is Add && expression.e1 is Const && expression.e2 is ConversionHistoryExpression) {
                val conversionHistoryExpression = expression.e2 as ConversionHistoryExpression
                val sourceCurrency = conversionHistoryExpression.source
                val targetCurrency = conversionHistoryExpression.target
                val rateAlert = RateAlert(
                    id = UUID.randomUUID().toString(),
                    apiName = api.name,
                    fromCurrency = sourceCurrency.code,
                    toCurrency = targetCurrency.code,
                    isRelative = isRelative,
                    value = (expression.e1 as Const).number.abs()
                )

                val sourceValue = 1.toFixedScaleBigDecimal().toStr(settings.decimalDigits)
                val targetValue = if (isRelative) "?.${"?".repeat(settings.decimalDigits)}" else rateAlert.value.toStr(settings.decimalDigits)
                val deltaValue = if (!isRelative) "?.${"?".repeat(settings.decimalDigits)}" else rateAlert.value.toStr(settings.decimalDigits)

                val newSettings = settings.copy(alerts = settings.alerts?.let { it + rateAlert } ?: listOf(rateAlert))
                context.settingsStore.save(chat.id.toString(), newSettings)

                val messages = BotMessages.alertCommand.of(settings.language)
                    .replace("{api_name}", BotMessages.apiDisplayNames.of(settings.language).getValue(api.name))
                    .replace("{source_emoji}", sourceCurrency.emoji)
                    .replace("{source_currency}", sourceCurrency.code)
                    .replace("{source_value}", sourceValue)

                    .replace("{target_emoji}", targetCurrency.emoji)
                    .replace("{target_currency}", targetCurrency.code)
                    .replace("{target_value}", targetValue)

                    .replace("{delta_value}", deltaValue)

                val formattedMessage = BotTextOutput(messages)
                context.sender.sendChatMessage(chat.id.toString(), formattedMessage)
            } else {
                val errorMessages = BotMessages.errors.of(settings.language)
                context.sender.sendChatMessage(chat.id.toString(), BotSimpleErrorOutput(errorMessages.unexpectedError))
                context.statsStore.logExchangeErrorRequest("rateAlertUnexpectedError", inlineRequest = false)
                throw IllegalStateException("Unexpected expression type: ${expression::class.simpleName} for $query")
            }
        }

        is ErrorResult -> {
            val messages = BotMessages.errors.of(settings.language)
            val errorOutput = result.toBotOutput(query, messages)
            val formattedOutput = errorOutput.copy(errorMessage = errorOutput.errorMessage)
            logger.info("Invalid rate alert query provided: ${formattedOutput.errorMessage} (at ${formattedOutput.errorPosition})")
            context.statsStore.logExchangeErrorRequest("rateAlert${result::class.simpleName}", inlineRequest = false)
            context.sender.sendChatMessage(chat.id.toString(), formattedOutput)
        }
    }
}

private suspend fun Message.handleMyHourlyRateCommand(
    text: String,
    settings: UserSettings,
    context: BotContext
) {
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
