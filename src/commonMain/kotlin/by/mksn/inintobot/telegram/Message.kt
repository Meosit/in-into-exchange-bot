package by.mksn.inintobot.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Long,
    val text: String? = null,
    val from: User? = null,
    val date: Int,
    val chat: Chat,
    @SerialName("reply_to_message") val replyToMessage: Message? = null
)