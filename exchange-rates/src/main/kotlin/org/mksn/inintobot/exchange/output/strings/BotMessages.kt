package org.mksn.inintobot.exchange.output.strings

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer


object BotMessages {
    const val maxOutputLength: Int = 4096
    const val maxErrorLineLength: Int = 32
    val supportedLanguages: Map<String, String> = mapOf(
        "en" to "English",
        "ru" to "Русский",
        "be" to "Беларуская",
        "uk" to "Українська",
        "pl" to "Polski",
    )

    val startCommand = LocalizedTextResource("start.md")
    val helpCommand = LocalizedTextResource("help.md")
    val apiCommand = LocalizedTextResource("api.md")
    val apisCommand = LocalizedTextResource("apis.md")
    val apiStatusCommand = LocalizedTextResource("api-status.md")
    val patternsCommand = LocalizedTextResource("patterns.md")
    val apiDisplayNames = LocalizedJsonResource("api-names.json", MapSerializer(String.serializer(), String.serializer()))
    val query = LocalizedJsonResource("query.json", QueryStrings.serializer())
    val timeUnitNames = LocalizedJsonResource("time-unit-names.json", TimeUnitNames.serializer())
    val errors = LocalizedJsonResource("errors.json", ErrorMessages.serializer())
    val settings = LocalizedJsonResource("settings.json", SettingsStrings.serializer())
}