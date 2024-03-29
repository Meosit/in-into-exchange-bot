package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.lexer.Tokenizer

/**
 * A simple tokenizer which accepts only single line input, all new lines are replaced with just space symbol.
 *
 * Tokenizing is performed by [delegate] tokenizer.
 */
class SingleLineTokenizer(private val delegate: Tokenizer) : Tokenizer {
    override val tokens = delegate.tokens

    override fun tokenize(input: String): TokenMatchesSequence {
        return delegate.tokenize(input.trim().replace('\n', ' '))
    }

}