package org.mksn.inintobot.exchange.output

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import java.util.*

class BotOutputSender(private val httpClient: HttpClient, apiToken: String) {

    private val logger = LoggerFactory.getLogger(BotOutputSender::class.simpleName)
    private val apiUrl = "https://api.telegram.org/bot$apiToken"

    private suspend inline fun <reified T: ApiResponse>postWithBackpressure(maxTries: Int = 5, block: HttpRequestBuilder.() -> Unit): T {
        var tries = 0
        while (tries < maxTries) {
            try {
                return httpClient.post(block).body()
            } catch (e: ClientRequestException) {
                val response = e.response.body<T>()
                val parameters = response.parameters
                when {
                    response.errorCode == 429 && parameters?.retryAfter != null -> {
                        val retryAfter = parameters.retryAfter
                        logger.warn("Got '${response.description}'. Retrying request in $retryAfter seconds")
                        delay(retryAfter * 1000 + 500)
                        tries++
                    }
                    response.errorCode == 403 -> {
                        logger.warn("Got '${response.description}'. Ignoring it")
                        return response
                    }
                    else -> throw e
                }
            }
        }
        throw IllegalStateException("Telegram request reached limit of $maxTries tries")
    }

    suspend fun editChatMessage(chatId: String, messageId: Long, output: BotOutput) {
        postWithBackpressure<MessageApiResponse> {
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
        postWithBackpressure<BooleanApiResponse> {
            url("$apiUrl/deleteMessage")
            parameter("chat_id", chatId)
            parameter("message_id", messageId)
        }
    }

    suspend fun sendChatMessage(chatId: String, output: BotOutput, replyMessageId: Long? = null, disableNotification: Boolean = false) {
        postWithBackpressure<MessageApiResponse> {
            url("$apiUrl/sendMessage")
            parameter("text", output.markdown())
            parameter("parse_mode", "Markdown")
            parameter("disable_web_page_preview", true)
            parameter("disable_notification", disableNotification)
            parameter("chat_id", chatId)
            replyMessageId?.let { parameter("reply_to_message_id", it.toString()) }
            val keyboardJson = output.keyboardJson()
            keyboardJson?.let { parameter("reply_markup", it) }
        }
    }

    suspend fun pingCallbackQuery(queryId: String) {
        postWithBackpressure<BooleanApiResponse> {
            url("$apiUrl/answerCallbackQuery")
            parameter("callback_query_id", queryId)
        }
    }

    suspend fun sendInlineQuery(queryId: String, startButtonLabel: String?, vararg outputs: BotOutput) {
        val jsonQueryResults = outputs
            .joinToString(prefix = "[", postfix = "]") { it.toInlineQueryResultArticle() }
        postWithBackpressure<BooleanApiResponse> {
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