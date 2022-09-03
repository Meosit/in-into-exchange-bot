package org.mksn.inintobot.currency

object Currencies {
    val ALL = mapOf(
        "USD" to Currency(code = "USD", emoji = "🇺🇸"),
        "EUR" to Currency(code = "EUR", emoji = "🇪🇺"),
        "BYN" to Currency(code = "BYN", emoji = "🇧🇾"),
        "RUB" to Currency(code = "RUB", emoji = "🇷🇺"),
        "UAH" to Currency(code = "UAH", emoji = "🇺🇦"),
        "PLN" to Currency(code = "PLN", emoji = "🇵🇱"),
        "CZK" to Currency(code = "CZK", emoji = "🇨🇿"),
        "GBP" to Currency(code = "GBP", emoji = "🇬🇧"),
        "JPY" to Currency(code = "JPY", emoji = "🇯🇵"),
        "CNY" to Currency(code = "CNY", emoji = "🇨🇳"),
        "KRW" to Currency(code = "KRW", emoji = "\uD83C\uDDF0\uD83C\uDDF7"),
        "HKD" to Currency(code = "HKD", emoji = "\uD83C\uDDED\uD83C\uDDF0"),
        "ISK" to Currency(code = "ISK", emoji = "🇮🇸"),
        "DKK" to Currency(code = "DKK", emoji = "🇩🇰"),
        "SEK" to Currency(code = "SEK", emoji = "🇸🇪"),
        "NOK" to Currency(code = "NOK", emoji = "🇳🇴"),
        "TRY" to Currency(code = "TRY", emoji = "🇹🇷"),
        "AMD" to Currency(code = "AMD", emoji = "\uD83C\uDDE6\uD83C\uDDF2"),
        "KZT" to Currency(code = "KZT", emoji = "🇰🇿"),
        "KGS" to Currency(code = "KGS", emoji = "\uD83C\uDDF0\uD83C\uDDEC"),
        "ILS" to Currency(code = "ILS", emoji = "\uD83C\uDDEE\uD83C\uDDF1"),
        "AED" to Currency(code = "AED", emoji = "\uD83C\uDDE6\uD83C\uDDEA"),
        "GEL" to Currency(code = "GEL", emoji = "\uD83C\uDDEC\uD83C\uDDEA"),
        "THB" to Currency(code = "THB", emoji = "\uD83C\uDDF9\uD83C\uDDED"),
        "IDR" to Currency(code = "IDR", emoji = "\uD83C\uDDEE\uD83C\uDDE9"),
        "INR" to Currency(code = "INR", emoji = "\uD83C\uDDEE\uD83C\uDDF3"),
        "VND" to Currency(code = "VND", emoji = "\uD83C\uDDFB\uD83C\uDDF3"),
        "CHF" to Currency(code = "CHF", emoji = "🇨🇭"),
        "HUF" to Currency(code = "HUF", emoji = "\uD83C\uDDED\uD83C\uDDFA"),
        "RON" to Currency(code = "RON", emoji = "\uD83C\uDDF7\uD83C\uDDF4"),
        "BGN" to Currency(code = "BGN", emoji = "🇧🇬"),
        "MDL" to Currency(code = "MDL", emoji = "\uD83C\uDDF2\uD83C\uDDE9"),
        "HRK" to Currency(code = "HRK", emoji = "\uD83C\uDDED\uD83C\uDDF7"),
        "CAD" to Currency(code = "CAD", emoji = "🇨🇦"),
        "MXN" to Currency(code = "MXN", emoji = "\uD83C\uDDF2\uD83C\uDDFD"),
        "AUD" to Currency(code = "AUD", emoji = "\uD83C\uDDE6\uD83C\uDDFA"),
        "BTC" to Currency(code = "BTC", emoji = "⛓"),
        "ETH" to Currency(code = "ETH", emoji = "\uD83D\uDCA0"),
    )

    fun forCode(code: String): Currency = ALL.getValue(code)
    fun forCodeOrNull(code: String): Currency? = ALL[code]
}