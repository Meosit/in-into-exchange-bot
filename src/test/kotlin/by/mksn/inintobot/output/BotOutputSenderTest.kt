package by.mksn.inintobot.output

import by.mksn.inintobot.test.fullUrl
import by.mksn.inintobot.test.fullUrlWithoutQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BotOutputSenderTest {

    private val testToken = "TEST_TOKEN"
    private val apiUrl = "https://api.telegram.org/bot$testToken"
    private val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))

    private fun testEngine(method: String, assertParametersBlock: Parameters.() -> Unit): HttpClient =
        HttpClient(MockEngine) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }
            engine {
                addHandler { request ->
                    when (request.url.fullUrlWithoutQuery) {
                        "$apiUrl/$method" -> {
                            request.url.parameters.assertParametersBlock()
                            val responseHeaders =
                                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            respond("success", headers = responseHeaders)
                        }
                        else -> error("Unhandled ${request.url.fullUrl}")
                    }
                }
            }
        }

    @Test
    fun testChatMessage() {
        val chatId = "1"
        val replyMessageId = 222L
        val method = "sendMessage"

        val botOutput = object : BotOutput {
            override fun inlineTitle() = "testInlineOutput"
            override fun inlineDescription() = "testInline\"Description\""
            override fun inlineThumbUrl(): String = "url.com"
            override fun markdown() = """
            test
            `markdown`
            \"lala\"
            """.trimIndent()
        }


        val httpClient = testEngine(method) {
            assertEquals("Markdown", get("parse_mode"))
            assertEquals("true", get("disable_web_page_preview"))

            assertEquals(chatId, get("chat_id"))
            assertEquals(replyMessageId.toString(), get("reply_to_message_id"))
            assertEquals(botOutput.markdown(), get("text"))
        }

        val sender = BotOutputSender(httpClient, testToken)

        runBlocking { sender.sendChatMessage(chatId, botOutput, replyMessageId) }
    }

    @Test
    fun testInlineQuery() {
        val queryId = "1"
        val method = "answerInlineQuery"

        val botOutput = object : BotOutput {
            override fun inlineTitle() = "testInlineOutput"
            override fun inlineDescription() = "testInline\"Description\""
            override fun inlineThumbUrl(): String = "url.com"
            override fun markdown() = "\ntest\n`markdown`\n\\\"lala\\\""
        }

        val expectedJson = """
        [{
          "type": "article",
          "id": "${UUID.randomUUID()}",
          "title": "testInlineOutput",
          "description": "testInline\"Description\"",
          "thumb_url": "url.com",
          "input_message_content": {
            "message_text": "\ntest\n`markdown`\n\\\"lala\\\"",
            "parse_mode": "Markdown",
            "disable_web_page_preview": true
          }
        }]""".trimIndent().replace("\n", "")

        val randomUuidRegex = "[\\w\\d\\-]{36}".toRegex()

        val httpClient = testEngine(method) {
            assertEquals(queryId, get("inline_query_id"))
            assertEquals(
                expectedJson.replace(randomUuidRegex, "random_id"),
                get("results")?.replace(randomUuidRegex, "random_id")
            )
        }

        val sender = BotOutputSender(httpClient, testToken)

        runBlocking { sender.sendInlineQuery(queryId, botOutput) }
    }

}