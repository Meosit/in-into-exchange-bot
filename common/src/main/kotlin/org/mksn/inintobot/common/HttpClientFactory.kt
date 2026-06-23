package org.mksn.inintobot.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException

private const val TRANSIENT_EMPTY_HEADER_MESSAGE = "header parser received no bytes"

fun defaultHttpClient(json: Json): HttpClient = HttpClient(Java) {
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpRequestRetry) {
        retryOnExceptionIf(maxRetries = 1) { _, cause ->
            cause is IOException && cause.hasMessage(TRANSIENT_EMPTY_HEADER_MESSAGE)
        }
        delayMillis { retry -> retry * 500L }
    }
}

private fun Throwable.hasMessage(fragment: String): Boolean =
    generateSequence(this as Throwable?) { it.cause }
        .any { it.message?.contains(fragment, ignoreCase = true) == true }
