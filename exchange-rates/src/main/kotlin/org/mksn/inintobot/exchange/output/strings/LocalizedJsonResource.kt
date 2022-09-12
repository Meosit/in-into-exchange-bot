package org.mksn.inintobot.exchange.output.strings

import kotlinx.serialization.DeserializationStrategy
import org.mksn.inintobot.exchange.json
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap


private fun loadResourceAsString(resourceBaseName: String): String = LocalizedJsonResource::class.java.classLoader
    .getResourceAsStream(resourceBaseName)
    .let { it ?: throw IllegalStateException("Null resource stream for $resourceBaseName") }
    .use { InputStreamReader(it).use(InputStreamReader::readText) }

class LocalizedJsonResource<T>(private val resourceFileName: String, private val deserializationStrategy: DeserializationStrategy<T>) {
    private val codeToLocale = ConcurrentHashMap<String, T>()
    fun of(language: String): T = codeToLocale.getOrPut(language) {
        val text = loadResourceAsString("message/$language/$resourceFileName")
        json.decodeFromString(deserializationStrategy, text)
    }
}

class LocalizedTextResource(private val resourceFileName: String) {
    private val codeToLocale = ConcurrentHashMap<String, String>()
    fun of(language: String): String = codeToLocale.getOrPut(language) {
        loadResourceAsString("message/$language/$resourceFileName")
    }
}