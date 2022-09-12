package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.misc.trimToLength

data class BotOutputWithMessage(
    val botOutput: BotOutput,
    val messageMarkdown: String
) : BotOutput {
    override fun inlineTitle(): String = botOutput.inlineTitle()

    override fun inlineThumbUrl(): String = botOutput.inlineThumbUrl()

    override fun inlineDescription(): String = botOutput.inlineDescription()

    override fun markdown(): String =
        "${botOutput.markdown()}\n\n${messageMarkdown}"
            .trimToLength(BotMessages.maxOutputLength, "…")
}