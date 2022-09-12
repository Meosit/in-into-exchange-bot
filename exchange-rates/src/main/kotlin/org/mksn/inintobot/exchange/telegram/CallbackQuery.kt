package org.mksn.inintobot.exchange.telegram

import kotlinx.serialization.Serializable

@Serializable
data class CallbackQuery(
    val id: String,
    val from: TelegramUser,
    val message: Message? = null,
    val data: String
)