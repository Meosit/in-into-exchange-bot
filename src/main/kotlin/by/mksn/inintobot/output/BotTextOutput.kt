package by.mksn.inintobot.output

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.trimToLength

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

    override fun markdown(): String = markdownText.trimToLength(AppContext.maxOutputLength, "…")

    override fun keyboardJson() = keyboardJson
}