package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.lexer.Token
import org.mksn.inintobot.exchange.grammar.alias.AliasMatcher

/**
 * This token matches only if next letters occurrence can produce correct match from underlying [aliasMatcher]
 */
class AliasMatchToken constructor(
    name: String?,
    private val aliasMatcher: AliasMatcher<*>,
    ignore: Boolean,
) : Token(name, ignore) {

    override fun match(input: CharSequence, fromIndex: Int): Int {
        val candidate = input.subSequence(fromIndex, input.length).takeWhile { it.isLetter() }.toString()
        return if (aliasMatcher.matchOrNull(candidate) == null) 0 else candidate.length
    }

    override fun toString(): String = "${name ?: ""} [autocomplete ${aliasMatcher.totalAliases} values]" + if (ignored) " [ignorable]" else ""
}

fun aliasMatchToken(name: String, aliasMatcher: AliasMatcher<*>, ignore: Boolean = false): Token =
    AliasMatchToken(name, aliasMatcher, ignore)
