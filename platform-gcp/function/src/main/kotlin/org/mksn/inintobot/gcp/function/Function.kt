package org.mksn.inintobot.gcp.function

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import kotlinx.coroutines.runBlocking
import org.mksn.inintobot.common.HttpBotFunction

@Suppress("unused")
class Function: HttpFunction {

    private val botFunction: HttpBotFunction = HttpBotFunction.load()

    override fun service(request: HttpRequest, response: HttpResponse) = runBlocking {
        val statusCode = botFunction.serve(request.inputStream)
        response.setStatusCode(statusCode)
    }
}