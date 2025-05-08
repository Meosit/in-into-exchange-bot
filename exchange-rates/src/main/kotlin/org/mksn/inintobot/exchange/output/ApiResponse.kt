package org.mksn.inintobot.exchange.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mksn.inintobot.exchange.telegram.Message


interface ApiResponse {
    val ok: Boolean
    val errorCode: Int?
    val description: String?
    val parameters: ApiResponseParameters?
}

@Serializable
data class MessageApiResponse(
    override val ok: Boolean,
    @SerialName("error_code") override val errorCode: Int? = null,
    override val description: String? = null,
    override val parameters: ApiResponseParameters? = null,

    val result: Message? = null,
): ApiResponse

@Serializable
data class BooleanApiResponse(
    override val ok: Boolean,
    @SerialName("error_code") override val errorCode: Int? = null,
    override val description: String? = null,
    override val parameters: ApiResponseParameters? = null,

    val result: Boolean? = null,
): ApiResponse

@Serializable
data class ApiResponseParameters(
    @SerialName("retry_after") val retryAfter: Long? = null
)

