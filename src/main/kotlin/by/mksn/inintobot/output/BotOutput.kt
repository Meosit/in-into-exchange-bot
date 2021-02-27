package by.mksn.inintobot.output

interface BotOutput {
    fun inlineTitle(): String
    fun inlineDescription(): String
    fun inlineThumbUrl(): String
    fun markdown(): String
    fun keyboardJson(): String? = null
    fun toApiResponse(): ApiResponse = throw IllegalStateException("Api response not supported for this output")
}