package by.mksn.inintobot.output

interface BotOutput {
    fun inlineTitle(): String
    fun inlineDescription(): String
    fun inlineThumbUrl(): String
    fun markdown(): String
}