package by.mksn.inintobot.output

import by.mksn.inintobot.settings.UserSettings

data class BotSimpleErrorOutput(
    val errorMessage: String
) : BotOutput {
    override fun inlineTitle() = errorMessage
    override fun inlineThumbUrl() = "https://i.imgur.com/yTMgvf9.png"

    override fun inlineDescription() = errorMessage

    override fun markdown() = errorMessage

    override fun toApiResponse(settings: UserSettings) = ApiErrorResponse(errorMessage)
}
