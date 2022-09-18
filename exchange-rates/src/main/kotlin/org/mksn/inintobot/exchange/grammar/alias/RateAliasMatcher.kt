package org.mksn.inintobot.exchange.grammar.alias

import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.rate.RateApis

object RateAliasMatcher : AutocompleteAliasMatcher<RateApi>() {

    override val aliasCharacters: Set<Char>
    override val aliases: Map<String, RateApi>

    init {
        this.aliasCharacters = mutableSetOf()
        this.aliases = sequence {
            for (c in RateApis) {
                yield(c.name.lowercase() to c)
                aliasCharacters.addAll(c.name.lowercase().toSet())
                c.aliases.forEach { a ->
                    yield(a.lowercase() to c)
                    aliasCharacters.addAll(a.lowercase().toSet())
                }
            }
        }.toMap()
    }
}