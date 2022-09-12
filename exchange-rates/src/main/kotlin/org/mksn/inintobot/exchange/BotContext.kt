package org.mksn.inintobot.exchange

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.mksn.inintobot.exchange.output.BotOutputSender
import org.mksn.inintobot.exchange.settings.FirestoreUserSettingsStore
import org.mksn.inintobot.exchange.settings.UserSettingsStore
import org.mksn.inintobot.rates.store.ApiExchangeRateStore
import org.mksn.inintobot.rates.store.FirestoreApiExchangeRateStore

data class BotContext(
    val creatorId: String,
    val botToken: String,
    val rateStore: ApiExchangeRateStore = FirestoreApiExchangeRateStore(),
    val settingsStore: UserSettingsStore = FirestoreUserSettingsStore(),
    val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    val httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(json)
        }
    },
    val sender: BotOutputSender =  BotOutputSender(httpClient, botToken),
)
