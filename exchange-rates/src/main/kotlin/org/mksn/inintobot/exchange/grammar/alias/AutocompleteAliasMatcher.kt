package org.mksn.inintobot.exchange.grammar.alias


abstract class AutocompleteAliasMatcher<T> : AliasMatcher<T> {

    abstract val aliases: Map<String, T>
    abstract val aliasCharacters: Set<Char>

    override val totalAliases: Int get() = aliases.size

    override fun charCanBeMatched(char: Char): Boolean = isAutocompletePossibleMatch(char) || char in aliasCharacters

    override fun matchOrNull(candidate: String): T? = when (val alias = candidate.lowercase()) {
        "" -> null
        in aliases -> aliases[alias]
        // original value is a priority, then goes transliterated one, last if keyboard-switched
        else -> alias.length.takeIf { it > 1 }?.let {
            val root = alias.wordRoot()
            aliases[root]
                ?: aliases.firstNotNullOfOrNull { (k, v) -> v.takeIf { k.startsWith(root, true) } }
                ?: aliases.firstNotNullOfOrNull { (k, v) -> v.takeIf { k.transliterate().startsWith(root, true) } }
                ?: aliases.firstNotNullOfOrNull { (k, v) -> v.takeIf { k.switchKeyboard().startsWith(root, true) } }
        } ?: aliases[alias.transliterate().lowercase()] ?: aliases[alias.switchKeyboard().lowercase()]
    }

    override fun match(candidate: String): T = matchOrNull(candidate)
        ?: throw NoSuchElementException("Cannot match given candidate '${candidate}' to known ${aliases.size} aliases")

    companion object {
        // The 'native' typing mapping of cyrillic symbols to the corresponding latin symbols on the same keyboard buttons
        // (e.g. the person forgot to switch the keyboard to cyrillic, but started typing and vice versa)
        // NOT ALL keyboard letters included, just the buttons where letters in both languages present
        private const val CYRILLIC_KEYBOARD_LETTERS =
              """йцукенгшщзхъфывапролджэёячсмитьбюЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЁЯЧСМИТЬБЮ"""
        private const val LATIN_KEYBOARD_LETTERS =
              """qwertyuiop[]asdfghjkl;'\zxcvbnm,.QWERTYUIOP{}ASDFGHJKL:"|ZXCVBNM<>"""

        // Cyrillic-latin naive transliteration, only for letters which have exact one-letter alternative,
        // for example cyrillic 'Ш' transliterates to 'SH', hence not inclided
        private const val CYRILLIC_TRANSLIT_LETTERS =
            """абвгдезийклмнопрстyфцАБВГДЕЗИЙКЛМНОПРСТУФЦ"""
        private const val LATIN_TRANSLIT_LETTERS =
            """abvgdeziyklmnoprstufcABVGDEZIYKLMNOPRSTUFC"""

        private val wordEndings =
            arrayOf("ый", "ей", "ой", "ов", "ая", "ые", "ых", "ях", "ах", "ия", "ии", "ь", "а", "ы", "и", "е", "я", "s")

        /**
         * Naive implementation of getting word root,
         * good-enough for currencies case as we try to cover as much as possible
         */
        private fun String.wordRoot(): String = takeIf { length > 3 }?.let {
            with(lowercase()) {
                wordEndings
                    .firstOrNull { this.endsWith(it) }
                    ?.let { this.removeSuffix(it) }
                    ?: this
            }
        } ?: this

        private inline fun String.mapChars(transform: (Char) -> Char): String {
            val chars = toCharArray()
            var index = 0
            for (char in chars)
                chars[index++] = transform(char)
            return String(chars)
        }

        private fun isAutocompletePossibleMatch(char: Char) = char in LATIN_KEYBOARD_LETTERS || char in CYRILLIC_KEYBOARD_LETTERS

        /**
         * Returns a string, contained of chars from the **cyrillic** or **latin** keyboard layout using the same *physical button* as a comparison criteria.
         * Buttons which do not contain any letter are remain the same (space, numbers, etc.)
         * Examples:
         * * "ghbdtn" -> "привет"
         * * "вщддфкы" -> "dollars"
         * * "rub and eur" -> "руб анд еюр"
         */
        private fun String.switchKeyboard() = mapChars {
            val source = if (it < Char(128)) LATIN_KEYBOARD_LETTERS else CYRILLIC_KEYBOARD_LETTERS
            val target = if (it < Char(128)) CYRILLIC_KEYBOARD_LETTERS else LATIN_KEYBOARD_LETTERS
            val idx = source.indexOf(it)
            if (idx == -1) it else target[idx]
        }

        /**
         * Returns a string, transliterated of chars from the **cyrillic** or **latin**
         * Buttons which do not contain any letter are remain the same (space, numbers, etc.)
         * Examples:
         * * "привет" -> "privet"
         * * "доллар" -> "dollar"
         * * "h"
         */
        private fun String.transliterate() = mapChars {
            val source = if (it < Char(128)) LATIN_TRANSLIT_LETTERS else CYRILLIC_TRANSLIT_LETTERS
            val target = if (it < Char(128)) CYRILLIC_TRANSLIT_LETTERS else LATIN_TRANSLIT_LETTERS
            val idx = source.indexOf(it)
            if (idx == -1) it else target[idx]
        }

    }

}