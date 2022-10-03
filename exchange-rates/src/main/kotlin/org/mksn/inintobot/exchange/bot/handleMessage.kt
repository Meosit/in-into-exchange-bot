package org.mksn.inintobot.exchange.bot

import io.ktor.client.plugins.*
import io.ktor.http.*
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.bot.settings.Setting
import org.mksn.inintobot.exchange.output.BotSimpleErrorOutput
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.telegram.Message
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.logging.Logger


private val logger = Logger.getLogger("handleMessage")


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
                            .replace("{base}", "`${rateApi.base}`")
                            .replace("{aliases}", rateApi.aliases.joinToString { "`$it`" })
                            .replace("{unsupported}", if (rateApi.unsupported.isEmpty()) "`-/-`" else rateApi.unsupported.joinToString { "`$it`" })
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
        "/settings" -> {
            logger.info("Handling $text command")
            Setting.ROOT.handle(null, this, settings, context)
            context.statsStore.logBotCommandUsage(text)
        }
        "/donate" -> {
            logger.info("Handling $text command")
            context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(BotMessages.donateCommand.of(settings.language)))
            context.statsStore.logBotCommandUsage(text)
        }
        "/stats" -> {
            logger.info("Handling $text command")
            if (this.chat.id.toString() == context.creatorId) {
                val stats = context.statsStore.get()
                logger.info(stats.toString())
                val markdown = """
*Total*: `${stats.totalRequests}` (chat: `${stats.totalRequests - stats.inlineRequests}`; inline: `${stats.inlineRequests}`)
*Errors*: `${stats.totalRequestsErrors}` (chat: `${stats.totalRequestsErrors - stats.inlineRequestsErrors}`; inline: `${stats.inlineRequestsErrors}`)
*With History*: `${stats.totalRequestsWithHistory}` (chat: `${stats.totalRequestsWithHistory - stats.inlineRequestsWithHistory}`; inline: `${stats.inlineRequestsWithHistory}`)
*Errors*:
${stats.errorUsage.toListString()}
*Commands*:
${stats.botCommandUsage.toListString()}
*Expressions*:
${stats.expressionTypeUsage.toListString { it.name.lowercase().replace("_", "-") }}
*Rate Apis*:
${stats.requestsRateApiUsage.toListString(RateApi::name) }
*Base Currency*:
${stats.requestsBaseCurrencyUsage.toListString(Currency::code)}
*Involved Currency*:
${stats.requestsInvolvedCurrencyUsage.toListString(Currency::code)}
*Output Currency*:
${stats.requestsInvolvedCurrencyUsage.toListString(Currency::code)}

*With Settings*: `${stats.usersWithCustomizedSettings}`
*Settings Default Currency*:
${stats.settingsDefaultCurrencyUsage.toListString(Currency::code)}
*Settings Default Api*:
${stats.settingsDefaultRateApiUsage.toListString(RateApi::name) }
*Settings Output*:
${stats.settingsOutputCurrencyUsage.toListString(Currency::code)}
*Settings Language*:
${stats.settingsLanguageUsage.toListString()}
                """.trim()
                context.sender.sendChatMessage(chat.id.toString(), BotTextOutput(markdown))
                context.statsStore.logBotCommandUsage(text)
            } else {
                val outputs = handleBotExchangeQuery(isInline = false, text, settings, context)
                outputs.firstOrNull()?.let { context.sender.sendChatMessage(chat.id.toString(), it) }
                context.statsStore.logExchangeErrorRequest("unauthorizedAdminCommand", inlineRequest = false)
            }
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

private fun <T> Map<T, Long>.toListString(selector: (T) -> String = { it.toString() }) = asSequence()
    .sortedByDescending { it.value }.joinToString("\n") { (k, v) -> "- ${selector(k)}: `$v`" }