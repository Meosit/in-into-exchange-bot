package org.mksn.inintobot.exchange.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Update(
    @SerialName("update_id") val updateId: Long,
    val message: Message? = null,
    @SerialName("edited_message") val editedMessage: Message? = null,
    @SerialName("channel_post") val channelPost: Message? = null,
    @SerialName("edited_channel_post") val editedChannelPost: Message? = null,
    @SerialName("inline_query") val inlineQuery: InlineQuery? = null,
    @SerialName("callback_query") val callbackQuery: CallbackQuery? = null
) {
    fun userReadableName() = inlineQuery?.from?.userReadableName()
        ?: (message ?: editedMessage ?: callbackQuery?.message)?.chat?.userReadableName()
        ?: "Unknown username"
}
