package org.mksn.inintobot.exchange

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.common.store.UserAggregateStatsStore
import org.mksn.inintobot.common.store.UserSettingsStore
import org.mksn.inintobot.exchange.output.BotOutputSender

data class BotContext(
    val creatorId: String,
    val botToken: String,
    val botUsername: String,
    val storeProvider: StoreProvider = StoreProvider.load(),
    val rateStore: ApiExchangeRateStore = storeProvider.exchangeRateStore(),
    val settingsStore: UserSettingsStore = storeProvider.userSettingsStore(),
    val statsStore: UserAggregateStatsStore = storeProvider.userAggregateStatsStore(),
    val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    val httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(json)
        }
    },
    val sender: BotOutputSender =  BotOutputSender(httpClient, botToken),
)
