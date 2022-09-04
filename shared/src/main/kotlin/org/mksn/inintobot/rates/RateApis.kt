package org.mksn.inintobot.rates

import org.mksn.inintobot.currency.Currencies

object RateApis {
    val ALL = mapOf(
        "NBRB" to RateApi(
            name = "NBRB",
            base = Currencies.forCode("BYN"),
            url = "http://www.nbrb.by/API/ExRates/Rates?Periodicity=0",
            displayLink = "http://www.nbrb.by/",
            unsupported = setOf("ILS", "GEL", "ISK", "THB", "IDR", "VND", "HRK", "MXN", "AED", "BTC", "ETH", "HUF"),
            refreshHours = 1,
            // API became unavailable from outside of Belarus
            available = false
        ),
        "NBU" to RateApi(
            name = "NBU",
            base = Currencies.forCode("UAH"),
            url = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json",
            displayLink = "https://bank.gov.ua/",
            unsupported = setOf("BTC", "ETH", "ISK"),
            refreshHours = 1,
        ),
        "NBP" to RateApi(
            name = "NBP",
            base = Currencies.forCode("PLN"),
            url = "https://api.nbp.pl/api/exchangerates/tables/<table_type>/?format=json",
            displayLink = "https://nbp.pl/",
            unsupported = setOf("BTC", "ETH"),
            refreshHours = 1,
        ),
        "CBR" to RateApi(
            name = "CBR",
            base = Currencies.forCode("RUB"),
            url = "http://www.cbr.ru/scripts/XML_daily.asp",
            displayLink = "http://www.cbr.ru/",
            unsupported = setOf("ILS", "GEL", "ISK", "THB", "IDR", "VND", "HRK", "MXN", "AED", "BTC", "ETH"),
            refreshHours = 1,
        ),
        "ECB" to RateApi(
            name = "ECB",
            base = Currencies.forCode("EUR"),
            url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml",
            displayLink = "https://www.ecb.europa.eu/home/html/index.en.html",
            unsupported = setOf("BYN", "UAH", "KZT", "GEL", "VND", "MDL", "AMD", "KGS", "AED", "BTC", "ETH"),
            refreshHours = 1,
        ),
        "Fixer" to RateApi(
            name = "Fixer",
            base = Currencies.forCode("EUR"),
            url = "http://data.fixer.io/api/latest?access_key=${System.getenv("FIXER_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,KGS,AUD,AED",
            displayLink = "https://fixer.io/",
            unsupported = setOf("ETH"),
            refreshHours = 8,
            // currently disabled to avoid reaching request limit while old version of bot is running
            available = false
        ),
        "OpenExchangeRates" to RateApi(
            name = "OpenExchangeRates",
            base = Currencies.forCode("USD"),
            url = "https://openexchangerates.org/api/latest.json?app_id=${System.getenv("OPENEXCHANGERATES_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,KGS,AUD,AED",
            displayLink = "https://openexchangerates.org/",
            unsupported = setOf("ETH"),
            refreshHours = 24, // Was 1 hour, but currently is once a day while this bot is not active
        ),
        "TraderMade" to RateApi(
            name = "TraderMade",
            base = Currencies.forCode("USD"),
            url = "https://marketdata.tradermade.com/api/v1/live?api_key=${System.getenv("TRADERMADE_ACCESS_KEY")}&currency=USDEUR,USDRUB,USDPLN,USDCZK,USDGBP,USDJPY,USDCNY,USDCHF,USDTRY,USDCAD,USDISK,USDDKK,USDSEK,USDNOK,USDILS,USDBTC,USDETH,USDHUF,USDTHB,USDIDR,USDINR,USDRON,USDHKD,USDKRW,USDVND,USDHRK,USDMXN,USDAUD,USDAED",
            displayLink = "https://tradermade.com/",
            unsupported = setOf("BYN", "BGN", "GEL", "AMD", "MDL", "KZT", "KGS", "UAH"),
            refreshHours = 24, // Was 2 hours, but currently is once a day while this bot is not active
        ),
        "Forex" to RateApi(
            name = "Forex",
            base = Currencies.forCode("USD"),
            url = "https://fcsapi.com/api-v2/forex/latest?access_key=${System.getenv("FOREX_ACCESS_KEY")}&symbol=USD/BYN,USD/EUR,USD/RUB,USD/UAH,USD/PLN,USD/CZK,USD/GBP,USD/JPY,USD/CNY,USD/KZT,USD/CHF,USD/BGN,USD/TRY,USD/CAD,USD/ISK,USD/DKK,USD/SEK,USD/NOK,USD/ILS,USD/BTC,USD/ETH,USD/GEL,USD/HUF,USD/MXN,USD/HRK,USD/VND,USD/KRW,USD/MDL,USD/HKD,USD/RON,USD/IDR,USD/INR,USD/THB,USD/AMD,USD/KGS,USD/AUE,USD/AED",
            displayLink = "https://fcsapi.com/",
            unsupported = setOf(),
            refreshHours = 24, // Was 2 hours, but currently is once a day while this bot is not active
        ),
    )

    val AVAILABLE = ALL.filter { it.value.available }

    fun fromName(name: String, onlyAvailable: Boolean = true): RateApi =
        (if (onlyAvailable) AVAILABLE else ALL).getValue(name)
}