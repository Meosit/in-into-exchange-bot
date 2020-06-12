package by.mksn.inintobot.output

import by.mksn.inintobot.misc.escapeMarkdown
import by.mksn.inintobot.misc.trimToLength

data class BotTextOutput(
    val markdown: String
) : BotOutput {
    override fun inlineTitle(): String = markdown.escapeMarkdown().trimToLength(30, "…")

    override fun inlineDescription(): String = markdown.escapeMarkdown()

    override fun markdown(): String = markdown
}