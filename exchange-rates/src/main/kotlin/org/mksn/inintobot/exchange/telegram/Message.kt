package org.mksn.inintobot.exchange.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Long,
    val text: String? = null,
    val from: TelegramUser? = null,
    val date: Long,
    val chat: Chat,
    @SerialName("reply_to_message") val replyToMessage: Message? = null,
    @SerialName("via_bot") val viaBot: TelegramUser? = null
)