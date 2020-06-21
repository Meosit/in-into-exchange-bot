package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.BotUser
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.settings.UserStore
import by.mksn.inintobot.telegram.Message
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private fun ZonedDateTime.toSimpleString() = formatter.format(this)

private fun BotUser.toChatString() = """
    User: ${if (name.contains(" ")) "`${name}`" else "@${name}"} (`${id}`)
    When: `${lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("UTC+3"))
    .toSimpleString()}`
    Query: `${lastQuery.trimToLength(25, "…")}`
    Requests: `${numRequests}` (chat: `${numRequests - inlineRequests}`; inline: `${inlineRequests}`)
    Settings: `${settings?.let { AppContext.json.stringify(UserSettings.serializer(), it) }}`
""".trimIndent()

suspend fun Message.handleAdminCommand(sender: BotOutputSender): Boolean = when (text) {
    "/reload" -> {
        AppContext.exchangeRates.reloadAll(AppContext.httpClient, AppContext.json)
        val markdown = AppContext.exchangeRates.whenUpdated.asSequence()
            .map { (api, updated) ->
                "${api.name}: ${updated.withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}"
            }
            .joinToString(separator = "\n", prefix = "Last updated:\n```\n", postfix = "\n```")
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/me" -> {
        val user = UserStore.userById(AppContext.creatorId.toLong())
        val markdown = user?.toChatString() ?: "Not found"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/last5" -> {
        val users = UserStore.lastUsed(5).drop(1)
        val markdown = users
            .joinToString(separator = "\n---\n", prefix = "Last 5 users except admin:\n") { it.toChatString() }
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/lasthour" -> {
        val users = UserStore.lastUsed(10, 1).drop(1)
        val markdown = if (users.isNotEmpty()) users
            .joinToString(separator = "\n---\n", prefix = "Users for the last hour:\n") { it.toChatString() }
        else "No users for the last hour"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/lastday" -> {
        val users = UserStore.lastUsed(100, 24).drop(1)
        val markdown = if (users.isNotEmpty()) users
            .joinToString(separator = "\n---\n", prefix = "Users for the last 24 hours:\n") { it.toChatString() }
        else "No users for the last 24 hours"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    else -> when {
        text != null && text.startsWith("/reload ") -> {
            val apiAlias = text.removePrefix("/reload ")
            val rateApi = AliasMatcher(AppContext.supportedApis).matchOrNull(apiAlias)
            val markdown = if (rateApi != null) {
                AppContext.exchangeRates.reloadOne(rateApi, AppContext.httpClient, AppContext.json)
                AppContext.exchangeRates.whenUpdated.asSequence()
                    .filter { it.key == rateApi }
                    .map { (api, updated) ->
                        "${api.name}: ${updated.withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}"
                    }
                    .ifEmpty { sequenceOf("API was never updated") }
                    .joinToString(separator = "\n", prefix = "Last updated:\n```\n", postfix = "\n```")
            } else {
                "Invalid API alias provided"
            }
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        text != null && text.startsWith("/user ") -> {
            val id = text.removePrefix("/user ")
            val markdown = try {
                val user = UserStore.userById(id.toLong())
                user?.toChatString() ?: "Not found"
            } catch (e: Exception) {
                "Unable to select: ${e::class.simpleName} ${e.message ?: e.cause?.message}"
            }
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        text != null && text.startsWith("/where ") -> {
            val whereClause = text.removePrefix("/where ")
            val markdown = try {
                val users = UserStore.usersByWhere(whereClause)
                users.joinToString(separator = "\n---\n", prefix = "Selected:\n") { it.toChatString() }
            } catch (e: Exception) {
                "Unable to select: ${e::class.simpleName} ${e.message ?: e.cause?.message}"
            }
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        else -> false
    }
}
