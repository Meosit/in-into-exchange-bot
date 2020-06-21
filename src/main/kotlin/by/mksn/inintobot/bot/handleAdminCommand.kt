package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserStore
import by.mksn.inintobot.telegram.Message
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private fun ZonedDateTime.toSimpleString() = formatter.format(this)

suspend fun Message.handleAdminCommand(sender: BotOutputSender): Boolean = when (text) {
    "/reload" -> {
        AppContext.exchangeRates.reload(AppContext.httpClient, AppContext.json)
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
        val markdown = if (user != null) """
            User: ${user.name}
            When: ${user.lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC)
            .withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}
            Query: ${user.lastQuery.trimToLength(25, "…")}
            Requests: ${user.numRequests} (chat: ${user.numRequests - user.inlineRequests}; inline: ${user.inlineRequests})
            Settings: ${user.settings}
        """.trimIndent() else "Not found"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/last5" -> {
        val users = UserStore.lastUsed(5).drop(1)
        val markdown = users
            .joinToString(separator = "\n---\n", prefix = "Last 5 users except admin:\n```\n", postfix = "\n```") { user ->
                """
                    User: ${user.name}
                    When: ${user.lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}
                    Query: ${user.lastQuery.trimToLength(25, "…")}
                """.trimIndent()
            }
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/lasthour" -> {
        val users = UserStore.lastUsed(10, 1).drop(1)
        val markdown = if (users.isNotEmpty()) users
            .joinToString(separator = "\n---\n", prefix = "Users for the last hour:\n```\n", postfix = "\n```") { user ->
                """
                    User: ${user.name}
                    When: ${user.lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}
                    Query: ${user.lastQuery.trimToLength(25, "…")}
                """.trimIndent()
            }
        else "No users for the last hour"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    "/lastday" -> {
        val users = UserStore.lastUsed(100, 24).drop(1)
        val markdown = if (users.isNotEmpty()) users
            .joinToString(separator = "\n---\n", prefix = "Users for the last 24 hours:\n```\n", postfix = "\n```") { user ->
                """
                    User: ${user.name}
                    When: ${user.lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ZoneId.of("UTC+3")).toSimpleString()}
                    Query: ${user.lastQuery.trimToLength(25, "…")}
                """.trimIndent()
            }
        else "No users for the last 24 hours"
        sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        true
    }
    else -> false
}
