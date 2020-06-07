package by.mksn.inintobot.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual fun <T> runTestBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    runBlocking(context, block)