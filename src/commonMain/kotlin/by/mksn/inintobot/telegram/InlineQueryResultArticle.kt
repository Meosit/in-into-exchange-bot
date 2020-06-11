package by.mksn.inintobot.telegram

import by.mksn.inintobot.misc.randomId32
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InlineQueryResultArticle(
    val type: String = "article",
    val id: String = randomId32(),
    val title: String,
    val description: String,
    @SerialName("input_message_content") val inputMessageContent: InputTextMessageContent
)