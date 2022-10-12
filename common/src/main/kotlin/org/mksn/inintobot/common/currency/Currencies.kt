package org.mksn.inintobot.common.currency

object Currencies : Iterable<Currency> {
    // @formatter:off
    private val ALL = mapOf(
        "USD" to Currency(code = "USD", emoji = "🇺🇸",                       aliases = arrayOf("$", "d", "д", "dollar", "bucks", "бакс", "доллар", "USA", "США")),
        "EUR" to Currency(code = "EUR", emoji = "🇪🇺",                       aliases = arrayOf("€", "e", "е", "euro", "евро", "European", "Евросоюз")),
        "BYN" to Currency(code = "BYN", emoji = "🇧🇾",                       aliases = arrayOf("b", "б", "br", "бун", "бур", "бр", "BYR", "Belarus", "Беларусь")),
        "RUB" to Currency(code = "RUB", emoji = "🇷🇺",                       aliases = arrayOf("₽", "r", "р", "rouble", "рубль", "RUR", "Russia", "Россия")),
        "UAH" to Currency(code = "UAH", emoji = "🇺🇦",                       aliases = arrayOf("₴", "u", "g", "grn", "гривна", "гривен", "грн", "Ukraine", "Украина")),
        "PLN" to Currency(code = "PLN", emoji = "🇵🇱",                       aliases = arrayOf("z", "з", "zloty", "złoty", "злотый", "PLZ", "Poland", "Польша")),
        "CZK" to Currency(code = "CZK", emoji = "🇨🇿",                       aliases = arrayOf("kč", "kc", "krn", "крона", "крн", "кц", "Czech", "Чехия",)),
        "GBP" to Currency(code = "GBP", emoji = "🇬🇧",                       aliases = arrayOf("£", "ф", "pound", "pnd", "pd", "фунт", "фнт", "UKL", "England", "Britain", "Англии", "Британия")),
        "JPY" to Currency(code = "JPY", emoji = "🇯🇵",                       aliases = arrayOf("¥", "yens", "йена", "Japan", "Япония")),
        "CNY" to Currency(code = "CNY", emoji = "🇨🇳",                       aliases = arrayOf("Ұ", "ю", "yuan", "юань", "China", "Китай")),
        "KRW" to Currency(code = "KRW", emoji = "\uD83C\uDDF0\uD83C\uDDF7", aliases = arrayOf("won", "вонa", "Korea", "Корея")),
        "HKD" to Currency(code = "HKD", emoji = "\uD83C\uDDED\uD83C\uDDF0", aliases = arrayOf("Hongkong", "Гонконг")),
        "ISK" to Currency(code = "ISK", emoji = "🇮🇸",                       aliases = arrayOf("Island", "Исландия")),
        "DKK" to Currency(code = "DKK", emoji = "🇩🇰",                       aliases = arrayOf("Denmark", "Дания")),
        "SEK" to Currency(code = "SEK", emoji = "🇸🇪",                       aliases = arrayOf("Sweden", "Швеция")),
        "NOK" to Currency(code = "NOK", emoji = "🇳🇴",                       aliases = arrayOf("Norway", "Норвегия")),
        "TRY" to Currency(code = "TRY", emoji = "🇹🇷",                       aliases = arrayOf("₺", "lira", "лира", "Turkey", "Турция")),
        "AMD" to Currency(code = "AMD", emoji = "\uD83C\uDDE6\uD83C\uDDF2", aliases = arrayOf("dram", "драм", "Armenia", "Армения")),
        "KZT" to Currency(code = "KZT", emoji = "🇰🇿",                       aliases = arrayOf("t", "т", "tenge", "тенге", "тнг", "Kazakhstan", "Казахстан")),
        "UZS" to Currency(code = "UZS", emoji = "\uD83C\uDDFA\uD83C\uDDFF", aliases = arrayOf("sum", "сум", "Uzbekistan", "Узбекистан")),
        "KGS" to Currency(code = "KGS", emoji = "\uD83C\uDDF0\uD83C\uDDEC", aliases = arrayOf("som", "сом", "Kyrgyzstan", "Кыргызстан")),
        "ILS" to Currency(code = "ILS", emoji = "\uD83C\uDDEE\uD83C\uDDF1", aliases = arrayOf("ш", "shekel", "шекель", "Israel", "Израиль")),
        "AED" to Currency(code = "AED", emoji = "\uD83C\uDDE6\uD83C\uDDEA", aliases = arrayOf("dirham", "дихрам", "UAE", "Emirates", "ОАЭ", "Эмираты", "د.إ")),
        "GEL" to Currency(code = "GEL", emoji = "\uD83C\uDDEC\uD83C\uDDEA", aliases = arrayOf("₾", "lari", "лари", "ლარი", "Georgia", "Грузия")),
        "THB" to Currency(code = "THB", emoji = "\uD83C\uDDF9\uD83C\uDDED", aliases = arrayOf("baht", "бат", "Thailand", "Тайланд")),
        "IDR" to Currency(code = "IDR", emoji = "\uD83C\uDDEE\uD83C\uDDE9", aliases = arrayOf("Indonesia", "Индонезия")),
        "INR" to Currency(code = "INR", emoji = "\uD83C\uDDEE\uD83C\uDDF3", aliases = arrayOf("₹", "rupee", "рупия", "Индия", "Индии")),
        "VND" to Currency(code = "VND", emoji = "\uD83C\uDDFB\uD83C\uDDF3", aliases = arrayOf("₫", "dong", "донг", "Vietnam", "Вьетнам")),
        "CHF" to Currency(code = "CHF", emoji = "🇨🇭",                       aliases = arrayOf("franks", "франк", "Switzerland", "Swiss", "Швейцария")),
        "HUF" to Currency(code = "HUF", emoji = "\uD83C\uDDED\uD83C\uDDFA", aliases = arrayOf("ft", "forint", "форинт", "Hungary", "Венгрия")),
        "RON" to Currency(code = "RON", emoji = "\uD83C\uDDF7\uD83C\uDDF4", aliases = arrayOf("leu", "lei", "лей", "лея", "леи", "Romaina", "Румыния")),
        "BGN" to Currency(code = "BGN", emoji = "🇧🇬",                       aliases = arrayOf("lev", "lv", "лев", "лв", "Bulgaria", "Болгария")),
        "MDL" to Currency(code = "MDL", emoji = "\uD83C\uDDF2\uD83C\uDDE9", aliases = arrayOf("Moldavia", "Moldova", "Молдова", "Молдавия")),
        "HRK" to Currency(code = "HRK", emoji = "\uD83C\uDDED\uD83C\uDDF7", aliases = arrayOf("kuna", "куна", "Croatia", "Хроватия")),
        "CAD" to Currency(code = "CAD", emoji = "🇨🇦",                       aliases = arrayOf("Canada", "Канада")),
        "MXN" to Currency(code = "MXN", emoji = "\uD83C\uDDF2\uD83C\uDDFD", aliases = arrayOf("Mexico", "pesos", "Мексика")),
        "AUD" to Currency(code = "AUD", emoji = "\uD83C\uDDE6\uD83C\uDDFA", aliases = arrayOf("Australia", "Австралия")),
        "BTC" to Currency(code = "BTC", emoji = "⛓",                       aliases = arrayOf("Bitcoin", "биткоин", "биток", "битки")),
        "ETH" to Currency(code = "ETH", emoji = "\uD83D\uDCA0",             aliases = arrayOf("Ethereum", "Эфириум", "эфир")),
    )
    // @formatter:on

    operator fun get(code: String): Currency = ALL.getValue(code)
    operator fun contains(code: String): Boolean = code in ALL
    override fun iterator(): Iterator<Currency> = ALL.values.iterator()

    val size = ALL.size

}