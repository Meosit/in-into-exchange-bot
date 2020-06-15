package by.mksn.inintobot.output

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.serialization.json.JsonLiteral
import java.util.*

class BotOutputSender(private val httpClient: HttpClient, apiToken: String) {

    private val apiUrl = "https://api.telegram.org/bot$apiToken"

    suspend fun sendChatMessage(chatId: String, output: BotOutput, replyMessageId: Long? = null) {
        httpClient.post<String> {
            url("$apiUrl/sendMessage")
            parameter("text", output.markdown())
            parameter("parse_mode", "Markdown")
            parameter("disable_web_page_preview", true)
            parameter("chat_id", chatId)
            replyMessageId?.let { parameter("reply_to_message_id", it.toString()) }
        }
    }

    suspend fun sendInlineQuery(queryId: String, vararg outputs: BotOutput) {
        val jsonQueryResults = outputs
            .joinToString(prefix = "[", postfix = "]") { it.toInlineQueryResultArticle() }
        httpClient.post<String> {
            url("$apiUrl/answerInlineQuery")
            parameter("inline_query_id", queryId)
            parameter("results", jsonQueryResults)
        }
    }

    private fun BotOutput.toInlineQueryResultArticle() = """
    {
      "type": "article",
      "id": "${UUID.randomUUID()}",
      "title": ${JsonLiteral(inlineTitle())},
      "description": ${JsonLiteral(inlineDescription())},
      "thumb_url": ${JsonLiteral(inlineThumbUrl())},
      "input_message_content": {
        "message_text": ${JsonLiteral(markdown())},
        "parse_mode": "Markdown",
        "disable_web_page_preview": true
      }
    }""".trimIndent().replace("\n", "")


}