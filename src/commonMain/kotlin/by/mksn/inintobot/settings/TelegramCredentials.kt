package by.mksn.inintobot.settings

import kotlinx.serialization.Serializable

@Serializable
data class TelegramCredentials(
    val tokens: Set<String>,
    val creatorId: String
)