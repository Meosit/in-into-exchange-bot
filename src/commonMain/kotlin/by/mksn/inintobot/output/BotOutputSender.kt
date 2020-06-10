package by.mksn.inintobot.output

import by.mksn.inintobot.telegram.Message
import by.mksn.inintobot.telegram.Response
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.serialization.json.JsonLiteral
import kotlin.random.Random

class BotOutputSender(private val httpClient: HttpClient, apiToken: String) {

    private val apiUrl = "https://api.telegram.org/bot$apiToken"

    suspend fun sendChatMessage(chatId: String, replyMessageId: String? = null, output: BotOutput): Response<Message> =
        httpClient.post {
            url("$apiUrl/sendMessage")
            parameter("text", output.markdown())
            parameter("parse_mode", "MarkdownV2")
            parameter("disable_web_page_preview", true)
            parameter("chat_id", chatId)
            replyMessageId?.let { parameter("reply_to_message_id", it) }
        }

    suspend fun sendInlineQuery(queryId: String, vararg outputs: BotOutput): Response<Boolean> {
        val jsonQueryResults = outputs
            .map { JsonLiteral(it.toInlineQueryResultArticle()) }
            .joinToString(separator = ", ", prefix = "[", postfix = "]")
        return httpClient.post {
            url("$apiUrl/sendMessage")
            parameter("inline_query_id", queryId)
            parameter("results", jsonQueryResults)
        }
    }

    private fun BotOutput.toInlineQueryResultArticle() = """{
      "type": "article",
      "id": "${randomId()}",
      "title": "${JsonLiteral(inlineTitle())}",
      "description": "${JsonLiteral(inlineDescription())}",
      "input_message_content": {
        "message_text": "${JsonLiteral(markdown())}",
        "parse_mode": "MarkdownV2",
        "disable_web_page_preview": true
      }
    }""".trimIndent().replace("\n", " ")

    companion object {
        private const val ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"
        private val RANDOM = Random.Default
        private fun randomId(): String {
            val length = 32
            val sb = StringBuilder(length)
            for (i in 0 until length)
                sb.append(ALLOWED_CHARACTERS[RANDOM.nextInt(ALLOWED_CHARACTERS.length)])
            return sb.toString()
        }

    }

}