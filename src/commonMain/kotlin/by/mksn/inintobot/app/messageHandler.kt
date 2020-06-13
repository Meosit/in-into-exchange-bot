package by.mksn.inintobot.app

import by.mksn.inintobot.misc.ResourceLoader
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.Message
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json


@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
suspend fun Message.handle(json: Json, httpClient: HttpClient, settings: UserSettings, botToken: String) {
    val sender = BotOutputSender(httpClient, botToken)
    when (text) {
        "", null -> {
            println("'$text' message text received")
            val errorMessages = ResourceLoader.errorMessages(json, settings.language)
            sender.sendChatMessage(chat.id.toString(), BotTextOutput(errorMessages.queryExpected))
        }
        "/start", "/help", "/patterns", "/apis" -> {
            println("Handling bot command $text")
            val message = when (text) {
                "/patterns" -> ResourceLoader.patternsMessage(settings.language)
                "/apis" -> ResourceLoader.apisMessage(settings.language)
                else -> ResourceLoader.helpMessage(settings.language)
            }
            sender.sendChatMessage(chat.id.toString(), BotTextOutput(message))
        }
        else -> {
            println("Handling '$text' chat message")
            val outputs = handleNonEmptyInput(text, json, httpClient, settings)
            outputs.firstOrNull()?.let { sender.sendChatMessage(chat.id.toString(), it) }
        }
    }
}