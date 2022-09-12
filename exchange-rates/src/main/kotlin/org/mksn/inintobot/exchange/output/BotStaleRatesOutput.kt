package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.misc.trimToLength

data class BotStaleRatesOutput(
    val botOutput: BotOutput,
    val apiName: String,
    val language: String
) : BotOutput {
    override fun inlineTitle(): String = botOutput.inlineTitle()

    override fun inlineThumbUrl(): String = botOutput.inlineThumbUrl()

    override fun inlineDescription(): String = botOutput.inlineDescription()

    private val message = BotMessages.errors.of(language).staleApiRates
        .format(BotMessages.apiDisplayNames.of(language).getValue(apiName))

    override fun markdown(): String {
        return "${botOutput.markdown()}\n\n_${message}_"
            .trimToLength(BotMessages.maxOutputLength, "…")
    }
}