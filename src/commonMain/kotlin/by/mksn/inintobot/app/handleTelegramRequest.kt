package by.mksn.inintobot.app

import by.mksn.inintobot.misc.BasicInfo
import by.mksn.inintobot.misc.ResourceLoader
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.Chat
import by.mksn.inintobot.telegram.Update
import by.mksn.inintobot.telegram.User
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.statement.readText
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * Handles the telegram bot [requestBody] with json-encoded [Update] for the specific [botToken]
 */
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
@UnstableDefault
suspend fun handleTelegramRequest(requestBody: String, botToken: String) {
    val json = Json(JsonConfiguration(ignoreUnknownKeys = true))
    val httpClient = createHttpClient(json)
    BasicInfo.load(json)

    val update = json.parse(Update.serializer(), requestBody)
    try {
        with(update) {
            val chat = message?.chat ?: editedMessage?.chat
            val user = inlineQuery?.from ?: message?.from ?: editedMessage?.from
            val settings = loadSettings(chat, user, json)
            when {
                inlineQuery != null -> inlineQuery.handle(json, httpClient, settings, botToken)
                message != null -> message.handle(json, httpClient, settings, botToken)
                editedMessage != null -> editedMessage.handle(json, httpClient, settings, botToken)
            }
        }
    } catch (e: Exception) {
        val cause = (e as? ResponseException)?.response?.readText() ?: e.message
        ?: "No exception message supplied (${e::class.simpleName})"
        val queryString = (update.message ?: update.editedMessage)?.text ?: update.inlineQuery?.query
        val user = update.inlineQuery?.from?.userReadableName()
            ?: (update.message ?: update.editedMessage)?.chat?.userReadableName()
        println("Error for query '$queryString': $cause")
        if ("query is too old" !in cause) {
            val sender = BotOutputSender(httpClient, botToken)
            val message = BotTextOutput("Error received.\n```\nQuery: $queryString\nUser: $user\n\nCause: $cause```")
            sender.sendChatMessage(BasicInfo.creatorId, message)
        }
    }
    return
}

fun loadSettings(chat: Chat?, user: User?, json: Json): UserSettings {
    val defaults = ResourceLoader.defaultSettings(json)
    val userId = chat?.id ?: user?.id
    val languageCode = user?.languageCode?.take(2)?.toLowerCase()
        .takeIf { BasicInfo.supportedLanguages.contains(it) } ?: defaults.language
    return if (userId != null) {
        // TODO load stored user settings
        defaults.copy(language = languageCode)
    } else {
        defaults.copy(language = languageCode)
    }
}

fun createHttpClient(json: Json) = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer(json)
    }
}

private fun Chat.userReadableName() =
    with(this) { username ?: "$firstName ${lastName ?: ""} ($id)" }

private fun User.userReadableName() =
    with(this) { username ?: "$firstName ${lastName ?: ""} ($id)" }