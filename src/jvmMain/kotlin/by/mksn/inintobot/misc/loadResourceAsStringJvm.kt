package by.mksn.inintobot.misc

import java.io.InputStreamReader

actual fun loadResourceAsString(resourceBaseName: String): String = ClassLoader
    .getSystemClassLoader()
    .getResourceAsStream(resourceBaseName)
    .let { it ?: throw IllegalStateException("Null resource stream") }
    .use { InputStreamReader(it).use(InputStreamReader::readText) }