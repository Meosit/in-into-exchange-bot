package org.mksn.inintobot.common.rate

import org.mksn.inintobot.common.currency.Currencies

object RateApis: Iterable<RateApi> {
    private val ALL = mapOf(
        // API became unreachable from outside of Belarus
        //"NBRB" to RateApi(
        //    name = "NBRB",
        //    base = Currencies["BYN"],
        //    url = "http://www.nbrb.by/API/ExRates/Rates?Periodicity=0",
        //    displayLink = "http://www.nbrb.by/",
        //    unsupported = setOf("ILS", "GEL", "ISK", "THB", "IDR", "VND", "HRK", "MXN", "AED", "BTC", "ETH", "HUF"),
        //    refreshHours = 1,
        //    staleTimeoutHours = 24,
        //),
        "NBU" to RateApi(
            name = "NBU",
            base = Currencies["UAH"],
            aliases = arrayOf(),
            url = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json",
            displayLink = "https://bank.gov.ua/",
            unsupported = setOf("BTC", "ETH", "ISK"),
            refreshHours = 1,
            staleTimeoutHours = 25,
        ),
        "NBP" to RateApi(
            name = "NBP",
            base = Currencies["PLN"],
            aliases = arrayOf(),
            url = "https://api.nbp.pl/api/exchangerates/tables/<table_type>/?format=json",
            displayLink = "https://nbp.pl/",
            unsupported = setOf("BTC", "ETH"),
            refreshHours = 1,
            staleTimeoutHours = 25,
        ),
        "CBR" to RateApi(
            name = "CBR",
            base = Currencies["RUB"],
            aliases = arrayOf("ЦБРФ"),
            url = "http://www.cbr.ru/scripts/XML_daily.asp",
            displayLink = "http://www.cbr.ru/",
            unsupported = setOf("ILS", "GEL", "ISK", "THB", "IDR", "VND", "HRK", "MXN", "AED", "BTC", "ETH"),
            refreshHours = 1,
            staleTimeoutHours = 25,
        ),
        "ECB" to RateApi(
            name = "ECB",
            base = Currencies["EUR"],
            aliases = arrayOf(),
            url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml",
            displayLink = "https://www.ecb.europa.eu/home/html/index.en.html",
            unsupported = setOf("BYN", "UAH", "KZT", "GEL", "VND", "MDL", "AMD", "UZS", "KGS", "AED", "BTC", "ETH"),
            refreshHours = 1,
            staleTimeoutHours = 25,
        ),
        // currently disabled to avoid reaching request limit while old version of bot is running
        //"Fixer" to RateApi(
        //    name = "Fixer",
        //    base = Currencies["EUR"],
        //      aliases = arrayOf("Fixer", "fix", "фиксер", "фикс"),
        //    url = "http://data.fixer.io/api/latest?access_key=${System.getenv("FIXER_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,KGS,AUD,AED",
        //    displayLink = "https://fixer.io/",
        //    unsupported = setOf("ETH"),
        //    refreshHours = 8,
        //    staleTimeoutHours = 9,
        //),
        "OpenExchangeRates" to RateApi(
            name = "OpenExchangeRates",
            base = Currencies["USD"],
            aliases = arrayOf("OER"),
            url = "https://openexchangerates.org/api/latest.json?app_id=${System.getenv("OPENEXCHANGERATES_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,UZS,KGS,AUD,AED",
            displayLink = "https://openexchangerates.org/",
            unsupported = setOf("ETH"),
            refreshHours = 24, // Was 1 hour, but currently is once a day while this bot is not active
            staleTimeoutHours = 2,
        ),
        "TraderMade" to RateApi(
            name = "TraderMade",
            base = Currencies["USD"],
            aliases = arrayOf("TM", "ТрейдерМейд"),
            url = "https://marketdata.tradermade.com/api/v1/live?api_key=${System.getenv("TRADERMADE_ACCESS_KEY")}&currency=USDEUR,USDRUB,USDPLN,USDCZK,USDGBP,USDJPY,USDCNY,USDCHF,USDTRY,USDCAD,USDISK,USDDKK,USDSEK,USDNOK,USDILS,USDBTC,USDETH,USDHUF,USDTHB,USDIDR,USDINR,USDRON,USDHKD,USDKRW,USDVND,USDHRK,USDMXN,USDAUD,USDAED",
            displayLink = "https://tradermade.com/",
            unsupported = setOf("BYN", "BGN", "GEL", "AMD", "MDL", "KZT", "UZS", "KGS", "UAH"),
            refreshHours = 24, // Was 2 hours, but currently is once a day while this bot is not active
            staleTimeoutHours = 3,
        ),
        "Forex" to RateApi(
            name = "Forex",
            base = Currencies["USD"],
            aliases = arrayOf("FX", "FC", "Форекс"),
            url = "https://fcsapi.com/api-v2/forex/latest?access_key=${System.getenv("FOREX_ACCESS_KEY")}&symbol=USD/BYN,USD/EUR,USD/RUB,USD/UAH,USD/PLN,USD/CZK,USD/GBP,USD/JPY,USD/CNY,USD/KZT,USD/CHF,USD/BGN,USD/TRY,USD/CAD,USD/ISK,USD/DKK,USD/SEK,USD/NOK,USD/ILS,USD/BTC,USD/ETH,USD/GEL,USD/HUF,USD/MXN,USD/HRK,USD/VND,USD/KRW,USD/MDL,USD/HKD,USD/RON,USD/IDR,USD/INR,USD/THB,USD/AMD,USD/UZS,USD/KGS,USD/AUE,USD/AED",
            displayLink = "https://fcsapi.com/",
            unsupported = setOf(),
            refreshHours = 24, // Was 2 hours, but currently is once a day while this bot is not active
            staleTimeoutHours = 3,
        ),
    )

    operator fun get(name: String): RateApi = ALL.getValue(name)
    operator fun contains(name: String) = name in ALL
    override fun iterator(): Iterator<RateApi> = ALL.values.iterator()
}