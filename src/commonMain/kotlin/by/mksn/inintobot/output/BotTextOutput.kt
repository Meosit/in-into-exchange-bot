package by.mksn.inintobot.output

import by.mksn.inintobot.misc.BasicInfo
import by.mksn.inintobot.misc.trimToLength

data class BotTextOutput(
    val markdown: String
) : BotOutput {
    override fun inlineTitle(): String =
        throw IllegalStateException("Inline mode not supported for this implementation")

    override fun inlineThumbUrl(): String =
        throw IllegalStateException("Inline mode not supported for this implementation")

    override fun inlineDescription(): String =
        throw IllegalStateException("Inline mode not supported for this implementation")

    override fun markdown(): String = markdown.trimToLength(BasicInfo.maxOutputLength, "…")
}