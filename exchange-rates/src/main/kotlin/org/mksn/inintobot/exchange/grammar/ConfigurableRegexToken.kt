package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.lexer.Token
import org.intellij.lang.annotations.Language
import java.util.regex.Matcher

/**
 * This is a copy of final [com.github.h0tk3y.betterParse.lexer.RegexToken] with ability to configure a underlying
 * JVM [Matcher] object.
 */
class ConfigurableRegexToken private constructor(
    name: String?,
    private val pattern: String,
    private val regex: Regex,
    ignore: Boolean,
    configurationBlock: Matcher.() -> Matcher
) : Token(name, ignore) {

    private val threadLocalMatcher = object : ThreadLocal<Matcher>() {
        override fun initialValue() = regex.toPattern().matcher("").configurationBlock()
    }

    private val matcher: Matcher get() = threadLocalMatcher.get()

    private companion object {
        private const val inputStartPrefix = "\\A"

        private fun prependPatternWithInputStart(patternString: String, options: Set<RegexOption>) =
            if (patternString.startsWith(inputStartPrefix))
                patternString.toRegex(options)
            else {
                val newlineAfterComments = if (RegexOption.COMMENTS in options) "\n" else ""
                val patternToEmbed = if (RegexOption.LITERAL in options) Regex.escape(patternString) else patternString
                ("$inputStartPrefix(?:$patternToEmbed$newlineAfterComments)").toRegex(options - RegexOption.LITERAL)
            }

    }

    constructor(
        name: String?,
        @Language("RegExp", prefix = "", suffix = "") patternString: String,
        ignore: Boolean,
        configurationBlock: Matcher.() -> Matcher
    ) : this(
        name,
        patternString,
        prependPatternWithInputStart(patternString, emptySet()),
        ignore,
        configurationBlock
    )

    override fun match(input: CharSequence, fromIndex: Int): Int {
        matcher.reset(input).region(fromIndex, input.length)

        if (!matcher.find()) {
            return 0
        }

        val end = matcher.end()
        return end - fromIndex
    }

    override fun toString(): String = "${name ?: ""} [$pattern]" + if (ignored) " [ignorable]" else ""
}


fun configurableRegexToken(name: String, @Language("RegExp", prefix = "", suffix = "") pattern: String, ignore: Boolean = false, useTransparentBounds: Boolean = false, useAnchoringBounds: Boolean = true): Token =
    ConfigurableRegexToken(name, pattern, ignore) { useTransparentBounds(useTransparentBounds).useAnchoringBounds(useAnchoringBounds) }
