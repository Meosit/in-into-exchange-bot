package by.mksn.inintobot.output

import by.mksn.inintobot.settings.UserSettings
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ApiExchangeRequest(val query: String, val settings: UserSettings = UserSettings())

@Serializable
data class ExchangeRow(val emoji: String, val code: String, val value: String)

@Serializable
sealed class ApiResponse(@Transient val code: HttpStatusCode = HttpStatusCode.OK)

@Serializable
@SerialName("success")
data class ApiSuccessResponse(val exchanges: List<ExchangeRow>, val header: String? = null, val apiName: String? = null): ApiResponse()

@Serializable
@SerialName("error")
data class ApiErrorResponse(val message: String, val rawInput: String? = null, val position: Int? = null): ApiResponse(HttpStatusCode.BadRequest)