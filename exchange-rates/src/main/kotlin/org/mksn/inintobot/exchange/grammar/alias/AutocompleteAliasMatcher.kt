package org.mksn.inintobot.exchange.grammar.alias


abstract class AutocompleteAliasMatcher<out T> : AliasMatcher<T> {

    abstract val aliases: Map<String, T>

    override val totalAliases: Int get() = aliases.size

    override fun matchOrNull(candidate: String): T? = when (candidate) {
        "" -> null
        in aliases -> aliases[candidate]
        // original value is a priority, then goes transliterated one, last if keyboard-switched
        else -> with(aliases) {
            val root = candidate.wordRoot()
            firstNotNullOfOrNull { (k, v) -> v.takeIf { k.startsWith(root, true) } }
                ?: firstNotNullOfOrNull { (k, v) -> v.takeIf { k.transliterate().startsWith(root, true) } }
                ?: firstNotNullOfOrNull { (k, v) -> v.takeIf { k.switchKeyboard().startsWith(root, true) } }
        }
    }

    override fun match(candidate: String): T = matchOrNull(candidate)
        ?: throw NoSuchElementException("Cannot autocomplete given candidate '${candidate}' to known ${aliases.size} aliases")

    companion object {
        // The 'native' typing mapping of cyrillic symbols to the corresponding latin symbols on the same keyboard buttons
        // (e.g. the person forgot to switch the keyboard to cyrillic, but started typing and vice versa)
        // NOT ALL keyboard letters included, just the buttons where letters in both languages present
        private const val CYRILLIC_KEYBOARD_LETTERS =
            """йцукенгшщзфывапролдячсмитьЙЦУКЕНГШЩЗФЫВАПРОЛДЯЧСМИТЬ"""
        private const val LATIN_KEYBOARD_LETTERS =
            """qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"""

        // Cyrillic-latin naive transliteration, only for letters which have exact one-letter alternative,
        // for example cyrillic 'Ш' transliterates to 'SH', hence not inclided
        private const val CYRILLIC_TRANSLIT_LETTERS =
            """абвгдезиклмнопрстyфцАБВГДЕЗИЙКЛМНОПРСТУФЦ"""
        private const val LATIN_TRANSLIT_LETTERS =
            """abvgdeziklmnoprstufcABVGDEZIYKLMNOPRSTUFC"""

        private val wordEndings =
            arrayOf("ый", "ей", "oй", "oв", "ая", "ые", "ых", "ях", "ах", "ия", "ии", "ь", "a", "ы", "и", "е", "я", "s")

        /**
         * Naive implementation of getting word root,
         * good-enough for currencies case as we try to cover as much as possible
         */
        fun String.wordRoot(): String = takeIf { length > 3 }?.let {
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