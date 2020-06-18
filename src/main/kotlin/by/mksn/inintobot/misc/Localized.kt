package by.mksn.inintobot.misc

class Localized<T>(private val codeToLocality: Map<String, T>) {

    constructor(supportedLocales: Iterable<String>, localizer: (String) -> T) :
            this(supportedLocales.asSequence().map { it to localizer(it) }.toMap())

    fun of(languageCode: String) = codeToLocality.getValue(languageCode)
}