package by.mksn.inintobot.test

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.Promise

actual fun runTestBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): dynamic = GlobalScope.promise(context = context, block = block)
