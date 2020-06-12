package by.mksn.inintobot.output

import by.mksn.inintobot.misc.randomId32
import by.mksn.inintobot.telegram.Chat
import by.mksn.inintobot.telegram.Message
import by.mksn.inintobot.telegram.Response
import by.mksn.inintobot.test.fullUrl
import by.mksn.inintobot.test.fullUrlWithoutQuery
import by.mksn.inintobot.test.runTestBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableDefault
class BotOutputSenderTest {

    private val testToken = "TEST_TOKEN"
    private val apiUrl = "https://api.telegram.org/bot$testToken"
    private val json = Json(JsonConfiguration(ignoreUnknownKeys = true))

    private fun testEngine(method: String, response: String, assertParametersBlock: Parameters.() -> Unit): HttpClient =
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
                            respond(response, headers = responseHeaders)
                        }
                        else -> error("Unhandled ${request.url.fullUrl}")
                    }
                }
            }
        }

    private fun <T> successResponseJson(result: T, resultSerializer: KSerializer<T>): String =
        json.stringify(
            Response.serializer(resultSerializer),
            Response(result = result, ok = true, errorDescription = null, errorCode = null)
        )

    @Test
    fun testChatMessage() {
        val chatId = "1"
        val replyMessageId = 222L
        val method = "sendMessage"

        val botOutput = object : BotOutput {
            override fun inlineTitle() = "testInlineOutput"
            override fun inlineDescription() = "testInline\"Description\""
            override fun markdown() = """
            test
            `markdown`
            \"lala\"
            """.trimIndent()
        }

        val message = Message(messageId = 1, date = 12345, chat = Chat(42, "type"))


        val httpClient = testEngine(method, successResponseJson(message, Message.serializer())) {
            assertEquals("MarkdownV2", get("parse_mode"))
            assertEquals("true", get("disable_web_page_preview"))

            assertEquals(chatId, get("chat_id"))
            assertEquals(replyMessageId.toString(), get("reply_to_message_id"))
            assertEquals(botOutput.markdown(), get("text"))
        }

        val sender = BotOutputSender(httpClient, testToken)

        val (result, _, _, _) = runTestBlocking { sender.sendChatMessage(chatId, botOutput, replyMessageId) }

        assertEquals(message, result)
    }

    @Test
    fun testInlineQuery() {
        val queryId = "1"
        val method = "answerInlineQuery"

        val botOutput = object : BotOutput {
            override fun inlineTitle() = "testInlineOutput"
            override fun inlineDescription() = "testInline\"Description\""
            override fun markdown() = "\ntest\n`markdown`\n\\\"lala\\\""
        }

        val expectedJson = """
        [{
          "type": "article",
          "id": "${randomId32()}",
          "title": "testInlineOutput",
          "description": "testInline\"Description\"",
          "input_message_content": {
            "message_text": "\ntest\n`markdown`\n\\\"lala\\\"",
            "parse_mode": "MarkdownV2",
            "disable_web_page_preview": true
          }
        }]""".trimIndent().replace("\n", "")

        val randomIntRegex = "[\\w\\d]{32}".toRegex()

        val httpClient = testEngine(method, successResponseJson(true, Boolean.serializer())) {
            assertEquals(queryId, get("inline_query_id"))
            assertEquals(
                expectedJson.replace(randomIntRegex, "random_id"),
                get("results")?.replace(randomIntRegex, "random_id")
            )
        }

        val sender = BotOutputSender(httpClient, testToken)

        val (result, _, _, _) = runTestBlocking { sender.sendInlineQuery(queryId, botOutput) }

        assertEquals(true, result)
    }

}