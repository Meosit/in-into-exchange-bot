package by.mksn.inintobot.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual fun runTestBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> Unit): Unit =
    runBlocking(context, block)