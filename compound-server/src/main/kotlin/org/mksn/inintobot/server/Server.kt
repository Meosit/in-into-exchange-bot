package org.mksn.inintobot.server

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.exchange.BotFunction
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.rates.FetchFunction
import java.io.InputStream
import java.time.Duration
import java.time.LocalTime

fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)

fun Application.module() {
    val storeProvider: StoreProvider = StoreProvider.load()
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val httpClient = HttpClient(Java) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(json)
        }
    }
    val exchangeRateFunction = BotFunction(storeProvider, json, httpClient)
    val fetchRatesFunction = FetchFunction(storeProvider, json, httpClient)

    routing {
        get("/") {
            call.respond("What are you looking here?")
        }
        get("/_ah/warmup") {
            call.respond("Warmed up")
        }
        post("/handle/${exchangeRateFunction.botToken}") {
            val code = exchangeRateFunction.serve(call.receiveStream())
            call.respond(HttpStatusCode.fromValue(code))
        }
    }
    launch {
        val start = LocalTime.now()
        delay(Duration.between(start.plusMinutes(1).withSecond(0), start).toMillis())
        while (true) {
            val now = LocalTime.now()
            if (now.minute == 0) {
                runCatching { fetchRatesFunction.serve(InputStream.nullInputStream()) }
                    .recoverCatching {
                        exchangeRateFunction.botSender.sendChatMessage(
                            exchangeRateFunction.creatorId, BotTextOutput("```${it.message}```")
                        )
                    }
            }
            delay(60000)
        }
    }
}