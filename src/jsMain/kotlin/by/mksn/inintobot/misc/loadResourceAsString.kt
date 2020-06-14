package by.mksn.inintobot.misc

actual fun loadResourceAsString(resourceBaseName: String): String =
    js("require('./' + resourceBaseName)") as String