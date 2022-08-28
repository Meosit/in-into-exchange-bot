package org.mksn.inintobot.shared

fun sharedFunction(args: Array<String>): String {
    println("Program arguments: ${args.joinToString()}")
    return args.joinToString()
}