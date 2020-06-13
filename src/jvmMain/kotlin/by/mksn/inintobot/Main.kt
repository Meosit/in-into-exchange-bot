package by.mksn.inintobot

import by.mksn.inintobot.app.handleTelegramRequest
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import io.ktor.client.features.ClientRequestException
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import java.io.PrintWriter
import java.io.StringWriter


@Suppress("unused")
class Main : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @UnstableDefault
    @ExperimentalStdlibApi
    @ExperimentalUnsignedTypes
    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context?): APIGatewayV2HTTPResponse {
        println("Start handling request")
        val botToken: String? = input.queryStringParameters["token"]
        val requestBody: String? = input.body
        if (botToken != null && requestBody != null) {
            println("Body: ${requestBody.replace("\n", "")}")
            try {
                runBlocking { handleTelegramRequest(requestBody, botToken) }
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                (e as? ClientRequestException)?.response?.content?.let {
                    println(runBlocking { it.readUTF8Line() })
                }
                println("Uncaught exception: ${sw.toString().replace("\n", "")}")
            }
        }

        return APIGatewayV2HTTPResponse.builder().withStatusCode(200).build()
    }
}