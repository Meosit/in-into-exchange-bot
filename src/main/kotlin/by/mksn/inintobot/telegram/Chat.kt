package by.mksn.inintobot.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: Long,
    val type: String,
    val title: String? = null,
    val username: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val description: String? = null
)