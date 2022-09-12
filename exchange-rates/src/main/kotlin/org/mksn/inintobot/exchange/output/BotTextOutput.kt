package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.misc.trimToLength

data class BotTextOutput(
    val markdownText: String,
    val keyboardJson: String? = null
) : BotOutput {
    override fun inlineTitle(): String =
        throw IllegalStateException("Inline mode not supported for this implementation")

    override fun inlineThumbUrl(): String =
        throw IllegalStateException("Inline mode not supported for this implementation")

    override fun inlineDescription(): String =
        throw IllegalStateException("Inline mode not supported for this implementation")

    override fun markdown(): String = markdownText.trimToLength(BotMessages.maxOutputLength, "…")

    override fun keyboardJson() = keyboardJson
}