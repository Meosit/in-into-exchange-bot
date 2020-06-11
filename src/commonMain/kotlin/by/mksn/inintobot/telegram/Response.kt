package by.mksn.inintobot.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val result: T?,
    val ok: Boolean,
    @SerialName("error_code") val errorCode: Int?,
    @SerialName("description") val errorDescription: String?
)