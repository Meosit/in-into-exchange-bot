package org.mksn.inintobot.exchange.grammar.alias

import org.mksn.inintobot.rates.RateApi
import org.mksn.inintobot.rates.RateApis

object RateAliases : AutocompleteAliasMatcher<RateApi>() {

    override val aliases: Map<String, RateApi> = sequence {
        for (a in RateApis) {
            yield(a.name to a)
            a.aliases.forEach { it to a }
        }
    }.toMap()

}