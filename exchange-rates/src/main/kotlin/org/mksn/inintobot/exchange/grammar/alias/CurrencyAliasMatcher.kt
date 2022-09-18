package org.mksn.inintobot.exchange.grammar.alias

import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency

object CurrencyAliasMatcher : AutocompleteAliasMatcher<Currency>() {

    override val aliases: Map<String, Currency>
    override val aliasCharacters: Set<Char>

    init {
        this.aliasCharacters = mutableSetOf()
        this.aliases = sequence {
            for (c in Currencies) {
                yield(c.code.lowercase() to c)
                aliasCharacters.addAll(c.code.lowercase().toSet())
                c.aliases.forEach { a ->
                    yield(a.lowercase() to c)
                    aliasCharacters.addAll(a.lowercase().toSet())
                }
            }
        }.toMap()
    }

}