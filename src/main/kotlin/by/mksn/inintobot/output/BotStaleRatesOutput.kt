package by.mksn.inintobot.output

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.settings.UserSettings

data class BotStaleRatesOutput(
    val botOutput: BotOutput,
    val apiName: String,
    val language: String
) : BotOutput {
    override fun inlineTitle(): String = botOutput.inlineTitle()

    override fun inlineThumbUrl(): String = botOutput.inlineThumbUrl()

    override fun inlineDescription(): String = botOutput.inlineDescription()

    private val message = AppContext.errorMessages.of(language).staleApiRates
        .format(AppContext.apiDisplayNames.of(language).getValue(apiName))

    override fun markdown(): String {
        return "${botOutput.markdown()}\n\n_${message}_"
            .trimToLength(AppContext.maxOutputLength, "…")
    }

    override fun toApiResponse(settings: UserSettings) = botOutput.toApiResponse(settings)
}