package by.mksn.inintobot.misc

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

expect fun loadResourceAsString(resourceBaseName: String): String

object ResourceLoader {
    private fun <T> Json.load(resourceBaseName: String, deserializer: DeserializationStrategy<T>): T =
        this.parse(deserializer, loadResourceAsString(resourceBaseName))

    fun currencies(json: Json): List<Currency> = json.load("currencies.json", Currency.serializer().list)
    fun apiConfigs(json: Json): List<RateApi> = json.load("apis.json", RateApi.serializer().list)

    fun helpMessage(): String = loadResourceAsString("telegram/help.md")
    fun patternsMessage(): String = loadResourceAsString("telegram/patterns.md")
}