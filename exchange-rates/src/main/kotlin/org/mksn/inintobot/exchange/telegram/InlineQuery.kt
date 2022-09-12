package org.mksn.inintobot.exchange.telegram

import kotlinx.serialization.Serializable

@Serializable
data class InlineQuery(
    val id: String,
    val from: TelegramUser,
    val query: String,
    val offset: String
)