package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.bot.settings.Setting
import by.mksn.inintobot.misc.TimeUnitNames
import by.mksn.inintobot.output.BotDeprecatedOutput
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.Message
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


private val logger = LoggerFactory.getLogger("handleMessage")

fun TimeUnitNames.nameOfMinutes(value: Long) = when (value % 100) {
    in 11L..20L -> minuteFiveTillTen
    else -> when (value % 10) {
        1L -> minuteOne
        in 2L..4L -> minuteTwoTillFour
        else -> minuteFiveTillTen
    }
}

fun TimeUnitNames.nameOfHours(value: Long) = when (value % 100) {
    in 11L..20L -> hourFiveTillTen
    else -> when (value % 10) {
        1L -> hourOne
        in 2L..4L -> hourTwoTillFour
        else -> hourFiveTillTen
    }
}

private fun encodeToStringDuration(time: ZonedDateTime, now: ZonedDateTime?, timeUnitNames: TimeUnitNames): String {
    val hours = ChronoUnit.HOURS.between(time, now)
    val minutes = ChronoUnit.MINUTES.between(time, now) - hours * 60
    return when {
        hours == 0L && minutes != 0L -> "`$minutes` ${timeUnitNames.nameOfMinutes(minutes)}"
        hours != 0L && minutes == 0L -> "`$hours` ${timeUnitNames.nameOfHours(hours)}"
        else -> "`$hours`${timeUnitNames.hourShort} `$minutes`${timeUnitNames.minuteShort}"
    }
}

suspend fun Message.handle(settings: UserSettings, sender: BotOutputSender, deprecatedBot: Boolean) {
    if (chat.id.toString() == AppContext.creatorId) {
        val handled = handleAdminCommand(sender)
        if (handled) return
    }
    when (text) {
        "", null -> {
            logger.info("'$text' message text received")
            val errorMessages = AppContext.errorMessages.of(settings.language)
            sender.sendChatMessage(chat.id.toString(), BotTextOutput(errorMessages.queryExpected))
        }
        "/start" -> {
            logger.info("Handling /start command")
            Setting.START_COMMAND.handle(null, this, settings, sender)
        }
        "/help", "/patterns", "/apis" -> {
            logger.info("Handling bot command $text")
            val message = with(AppContext.commandMessages.of(settings.language)) {
                val displayNames = AppContext.apiDisplayNames.of(settings.language)
                when (text) {
                    "/patterns" -> patterns
                        .replace("{currencies}", AppContext.supportedCurrencies
                            .joinToString("\n") { "- `${it.code}`:\n" + it.aliases.joinToString { a -> "`$a`" } })
                    "/apis" -> {
                        val apisContent = AppContext.supportedApis.joinToString("\n\n") { rateApi ->
                            val displayName = displayNames.getValue(rateApi.name)
                            api
                                .replace("{name}", displayName)
                                .replace("{link}", rateApi.displayLink)
                                .replace("{base}", "`${rateApi.base}`")
                                .replace("{aliases}", rateApi.aliases.joinToString { "`$it`" })
                                .replace("{unsupported}", if (rateApi.unsupported.isEmpty()) "`-/-`" else rateApi.unsupported.joinToString { "`$it`" })
                        }
                        apis.replace("{apis}", apisContent)
                    }
                    else -> help
                        .replace("{currency_count}", AppContext.supportedCurrencies.size.toString())
                        .replace("{currency_list}", AppContext.supportedCurrencies.joinToString { "`${it.code}`" })
                        .replace("{apis}", AppContext.supportedApis.joinToString { "[${displayNames.getValue(it.name)}](${it.displayLink})" })
                }
            }
            val formattedMessage = if (deprecatedBot) BotDeprecatedOutput(BotTextOutput(message), settings.language)
            else BotTextOutput(message)
            sender.sendChatMessage(chat.id.toString(), formattedMessage)
        }
        "/apistatus" -> {
            val statusFormat = AppContext.commandMessages.of(settings.language).apiStatus
            val apiDisplayNames = AppContext.apiDisplayNames.of(settings.language)
            val timeUnitNames = AppContext.timeUnitNames.of(settings.language)
            logger.info("Getting api status")
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val message = AppContext.exchangeRates.ratesStatus.values.joinToString(separator = "\n\n") {
                val ratesUpdatedString = encodeToStringDuration(it.ratesUpdated, now, timeUnitNames)
                val lastCheckedString = encodeToStringDuration(it.lastChecked, now, timeUnitNames)
                statusFormat.format(apiDisplayNames.getValue(it.api.name), ratesUpdatedString, lastCheckedString)
            }
            val formattedMessage = if (deprecatedBot) BotDeprecatedOutput(BotTextOutput(message), settings.language)
            else BotTextOutput(message)
            sender.sendChatMessage(chat.id.toString(), formattedMessage)
        }
        "/settings" -> {
            logger.info("Handling settings command")
            Setting.ROOT.handle(null, this, settings, sender)
        }
        else -> {
            logger.info("Handling '$text' chat message")
            val outputs = handleBotExchangeQuery(text, settings)
            outputs.firstOrNull()?.let {
                val formattedOutput = if (deprecatedBot) BotDeprecatedOutput(it, settings.language) else it
                sender.sendChatMessage(chat.id.toString(), formattedOutput)
            }
        }
    }
}