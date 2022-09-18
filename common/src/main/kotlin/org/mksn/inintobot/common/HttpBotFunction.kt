package org.mksn.inintobot.common

import java.io.InputStream
import java.util.*

interface HttpBotFunction {

    suspend fun serve(input: InputStream): Int

    companion object {
        fun load(): HttpBotFunction {
            val functions = HttpBotFunction::class.java.let { ServiceLoader.load(it, it.classLoader) }.toList()
            if (functions.size != 1) {
                throw Exception("Expected exactly one HttpBotFunction available, got ${functions.size}")
            }
            return functions[0]
        }
    }

}