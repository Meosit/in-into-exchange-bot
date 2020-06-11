package by.mksn.inintobot.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputTextMessageContent(
    @SerialName("message_text") val messageText: String,
    @SerialName("parse_mode") val parseMode: String = "MarkdownV2",
    @SerialName("disable_web_page_preview") val disableWebPagePreview: Boolean = false
)