package by.mksn.inintobot.output

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.trimToLength

data class BotDeprecatedOutput(
    val botOutput: BotOutput,
    val language: String
) : BotOutput {
    override fun inlineTitle(): String = botOutput.inlineTitle()

    override fun inlineThumbUrl(): String = botOutput.inlineThumbUrl()

    override fun inlineDescription(): String = botOutput.inlineDescription()

    override fun markdown(): String =
        "${botOutput.markdown()}\n\n_${AppContext.errorMessages.of(language).deprecatedBot}_"
            .trimToLength(AppContext.maxOutputLength, "…")
}