package by.mksn.inintobot.output

data class BotSimpleErrorOutput(
    val errorMessage: String
) : BotOutput {
    override fun inlineTitle() = errorMessage
    override fun inlineThumbUrl() = "https://i.imgur.com/yTMgvf9.png"

    override fun inlineDescription() = errorMessage

    override fun markdown() = errorMessage
}
