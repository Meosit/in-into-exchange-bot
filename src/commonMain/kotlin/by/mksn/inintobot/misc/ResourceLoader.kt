package by.mksn.inintobot.misc

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.output.ErrorMessages
import by.mksn.inintobot.output.TelegramStrings
import by.mksn.inintobot.settings.DefaultSettings
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

expect fun loadResourceAsString(resourceBaseName: String): String

object ResourceLoader {
    private fun <T> Json.load(resourceBaseName: String, deserializer: DeserializationStrategy<T>): T =
        this.parse(deserializer, loadResourceAsString(resourceBaseName))

    fun currencies(json: Json): List<Currency> = json.load("currencies.json", Currency.serializer().list)
    fun apiConfigs(json: Json): List<RateApi> = json.load("apis.json", RateApi.serializer().list)
    fun defaultSettings(json: Json): DefaultSettings = json.load("defaults.json", DefaultSettings.serializer())

    fun helpMessage(language: String): String = loadResourceAsString("message/$language/help.md")
    fun patternsMessage(language: String): String = loadResourceAsString("message/$language/patterns.md")
    fun apisMessage(language: String): String = loadResourceAsString("message/$language/apis.md")

    fun errorMessages(json: Json, language: String): ErrorMessages =
        json.load("message/$language/errors.json", ErrorMessages.serializer())

    fun telegramStrings(json: Json, language: String): TelegramStrings =
        json.load("message/$language/telegram.json", TelegramStrings.serializer())
}