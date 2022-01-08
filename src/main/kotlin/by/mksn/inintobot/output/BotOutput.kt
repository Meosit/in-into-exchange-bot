package by.mksn.inintobot.output

import by.mksn.inintobot.settings.UserSettings

interface BotOutput {
    fun inlineTitle(): String
    fun inlineDescription(): String
    fun inlineThumbUrl(): String
    fun markdown(): String
    fun keyboardJson(): String? = null
    fun toApiResponse(settings: UserSettings): ApiResponse = throw IllegalStateException("Api response not supported for this output")
}