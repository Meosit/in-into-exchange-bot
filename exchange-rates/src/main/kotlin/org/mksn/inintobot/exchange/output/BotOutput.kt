package org.mksn.inintobot.exchange.output

interface BotOutput {
    fun inlineTitle(): String
    fun inlineDescription(): String
    fun inlineThumbUrl(): String
    fun markdown(): String
    fun keyboardJson(): String? = null
}