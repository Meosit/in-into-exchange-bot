package by.mksn.inintobot.misc

import java.io.InputStreamReader

actual fun loadResourceAsString(resourceBaseName: String): String = ResourceLoader::class.java.classLoader
    .getResourceAsStream(resourceBaseName)
    .let { it ?: throw IllegalStateException("Null resource stream for $resourceBaseName") }
    .use { InputStreamReader(it).use(InputStreamReader::readText) }