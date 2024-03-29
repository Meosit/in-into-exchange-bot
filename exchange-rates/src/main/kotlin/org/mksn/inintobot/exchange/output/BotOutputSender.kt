package org.mksn.inintobot.exchange.output

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonPrimitive
import java.util.*

class BotOutputSender(private val httpClient: HttpClient, apiToken: String) {

    private val apiUrl = "https://api.telegram.org/bot$apiToken"

    suspend fun editChatMessage(chatId: String, messageId: Long, output: BotOutput) {
        httpClient.post {
            url("$apiUrl/editMessageText")
            parameter("text", output.markdown())
            parameter("parse_mode", "Markdown")
            parameter("disable_web_page_preview", true)
            parameter("chat_id", chatId)
            parameter("message_id", messageId)
            val keyboardJson = output.keyboardJson()
            keyboardJson?.let { parameter("reply_markup", it) }
        }
    }

    suspend fun deleteChatMessage(chatId: String, messageId: Long) {
        httpClient.post {
            url("$apiUrl/deleteMessage")
            parameter("chat_id", chatId)
            parameter("message_id", messageId)
        }
    }

    suspend fun sendChatMessage(chatId: String, output: BotOutput, replyMessageId: Long? = null) {
        httpClient.post {
            url("$apiUrl/sendMessage")
            parameter("text", output.markdown())
            parameter("parse_mode", "Markdown")
            parameter("disable_web_page_preview", true)
            parameter("chat_id", chatId)
            replyMessageId?.let { parameter("reply_to_message_id", it.toString()) }
            val keyboardJson = output.keyboardJson()
            keyboardJson?.let { parameter("reply_markup", it) }
        }
    }

    suspend fun pingCallbackQuery(queryId: String) {
        httpClient.post {
            url("$apiUrl/answerCallbackQuery")
            parameter("callback_query_id", queryId)
        }
    }

    suspend fun sendInlineQuery(queryId: String, startButtonLabel: String?, vararg outputs: BotOutput) {
        val jsonQueryResults = outputs
            .joinToString(prefix = "[", postfix = "]") { it.toInlineQueryResultArticle() }
        httpClient.post {
            url("$apiUrl/answerInlineQuery")
            parameter("inline_query_id", queryId)
            parameter("results", jsonQueryResults)
            if (startButtonLabel != null) {
                parameter("button", """{"text":${JsonPrimitive(startButtonLabel)},"start_parameter":"customise_settings"}""")
            }
        }
    }

    private fun BotOutput.toInlineQueryResultArticle() = """
    {
      "type": "article",
      "id": "${UUID.randomUUID()}",
      "title": ${JsonPrimitive(inlineTitle())},
      "description": ${JsonPrimitive(inlineDescription())},
      "thumb_url": ${JsonPrimitive(inlineThumbUrl())},
      "input_message_content": {
        "message_text": ${JsonPrimitive(markdown())},
        "parse_mode": "Markdown",
        "disable_web_page_preview": true
      }
    }""".trimIndent().replace("\n", "")


}