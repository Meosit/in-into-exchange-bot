package org.mksn.inintobot.exchange.grammar.alias

interface AliasMatcher<out T> {

    val totalAliases: Int

    fun matchOrNull(candidate: String): T?

    fun match(candidate: String): T = matchOrNull(candidate)
        ?: throw NoSuchElementException("Cannot autocomplete given candidate '${candidate}' to known ${totalAliases} aliases")

}
