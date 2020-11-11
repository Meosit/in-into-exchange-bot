package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.escapeMarkdown
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
    User: ${if (name.contains(" ")) "`${name.escapeMarkdown()}`" else "@${name.escapeMarkdown()}"} (`${id}`)
    When: `${lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("UTC+3"))
    .toSimpleString()}`
    Query: `${lastQuery.trimToLength(25, "…")}`
    Requests: `${numRequests}` (chat: `${numRequests - inlineRequests}`; inline: `${inlineRequests}`)
    Settings: `${settings?.let {
    if (settings != UserSettings(language = settings.language))
        AppContext.json.stringify(UserSettings.serializer(), it).escapeMarkdown()
    else "<same as default (${settings.language})>"
}}`
""".trimIndent()

suspend fun Message.handleAdminCommand(sender: BotOutputSender): Boolean = when (text) {
    "/me" -> {
        val user = UserStore.userById(AppContext.creatorId.toLong())
        val markdown = user?.toChatString() ?: "Not found"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    else -> when {
        text != null && text matches "/last\\d+".toRegex() -> {
            val limit = text.removePrefix("/last").toInt()
            val users = UserStore.lastUsed(limit + 1).drop(1)
            val markdown = if (users.isNotEmpty())
                users.joinToString(separator = "\n---\n", prefix = "Last ${limit} users except admin:\n") { it.toChatString() }
            else "No users"
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        text != null && text matches "/names\\d*".toRegex() -> {
            val limit = text.removePrefix("/names").toIntOrNull() ?: 100
            val users = UserStore.lastUsed(limit + 1).drop(1)
            val markdown = if (users.isNotEmpty())
                users.joinToString(separator = "- \n", prefix = "Last ${limit} user names except admin:\n") {
                    "${if (it.name.contains(" ")) "`${it.name.escapeMarkdown()}`" else "@${it.name.escapeMarkdown()}"} (`${it.id}`)"
                }
            else "No users"
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        text != null && text.startsWith("/reload") -> {
            val apiAlias = text.removePrefix("/reload")
            if (apiAlias.isEmpty()) {
                AppContext.exchangeRates.reloadAll(AppContext.httpClient, AppContext.json)
                val markdown = AppContext.exchangeRates.ratesStatus.asSequence()
                    .map { (api, updated) ->
                        "${api.name}: ${updated.lastChecked.withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}"
                    }
                    .joinToString(separator = "\n", prefix = "Last updated:\n```\n", postfix = "\n```")
                sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            } else {
                val rateApi = AliasMatcher(AppContext.supportedApis).matchOrNull(apiAlias)
                val markdown = if (rateApi != null) {
                    AppContext.exchangeRates.reloadOne(rateApi, AppContext.httpClient, AppContext.json)
                    AppContext.exchangeRates.ratesStatus.asSequence()
                        .filter { it.key == rateApi }
                        .map { (api, updated) ->
                            "${api.name}: ${updated.lastChecked.withZoneSameInstant(ZoneId.of("UTC+3"))
                                .toSimpleString()}"
                        }
                        .ifEmpty { sequenceOf("API was never updated") }
                        .joinToString(separator = "\n", prefix = "Last updated:\n```\n", postfix = "\n```")
                } else {
                    "Invalid API alias provided"
                }
                sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            }
            true
        }
        text != null && text.startsWith("/select") -> {
            val whereClause = text.removePrefix("/select")
            val markdown = try {
                val users = UserStore.usersGenericSelect(whereClause)
                users.joinToString(separator = "\n---\n", prefix = "Selected:\n") { it.toChatString() }
            } catch (e: Exception) {
                "Unable to select: ${e::class.simpleName} ${e.message ?: e.cause?.message}"
            }
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        text != null && text.startsWith("/stats") -> {
            val whereClause = text.removePrefix("/stats")
            val markdown = try {
                val stats = UserStore.userStats(whereClause)
                "Count: `${stats.count}`\nRequests: `${stats.requests}` (chat: `${stats.requests - stats.inlineRequests}`; inline: `${stats.inlineRequests}`)"
            } catch (e: Exception) {
                "Unable to select: ${e::class.simpleName} ${e.message ?: e.cause?.message}"
            }
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
            true
        }
        else -> false
    }
}
