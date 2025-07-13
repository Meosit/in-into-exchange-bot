package org.mksn.inintobot.common.rate

import org.mksn.inintobot.common.currency.Currencies
import java.time.LocalDate
import java.time.format.DateTimeFormatter


object RateApis : Iterable<RateApi> {
    private val ALL = mapOf(
        "NBRB" to RateApi(
            name = "NBRB",
            base = Currencies["BYN"],
            aliases = arrayOf(),
            url = "https://${System.getenv("NBRB_PROXY_HOST")}?Periodicity=0",
            displayLink = "https://myfin.by/bank/kursy_valjut_nbrb",
            unsupported = setOf("KRW", "HKD", "AMD", "RSD", "UZS", "ILS", "AED", "GEL", "THB", "IDR", "INR", "VND", "HUF", "RON", "HRK", "MXN", "BRL", "BTC", "ETH",),
            refreshHours = 2,
            staleTimeoutHours = 25,
            backFillInfo = RateApiBackFillInfo(
                url = "https://${System.getenv("NBRB_PROXY_HOST")}?Periodicity=0&OnDate=<date>",
                backFillLimit = LocalDate.of(2020, 1, 1),
                dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            ),
        ),
        "NBU" to RateApi(
            name = "NBU",
            base = Currencies["UAH"],
            aliases = arrayOf(),
            url = "https://bank.gov.ua/NBU_Exchange/exchange?json",
            displayLink = "https://bank.gov.ua/",
            unsupported = setOf("BTC", "ETH", "ISK"),
            refreshHours = 1,
            staleTimeoutHours = 25,
            backFillInfo = RateApiBackFillInfo(
                url = "https://bank.gov.ua/NBU_Exchange/exchange?json&date=<date>",
                backFillLimit = LocalDate.of(2020, 1, 1),
                dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            ),
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
            backFillInfo = RateApiBackFillInfo(
                // need to handle 404s for different tables
                url = "https://api.nbp.pl/api/exchangerates/tables/<table_type>/<date>/?format=json",
                backFillLimit = LocalDate.of(2020, 1, 1),
            ),
        ),
        "CBR" to RateApi(
            name = "CBR",
            base = Currencies["RUB"],
            aliases = arrayOf("ЦБРФ"),
            url = "http://www.cbr.ru/scripts/XML_daily.asp",
            displayLink = "http://www.cbr.ru/",
            unsupported = setOf("ILS", "GEL", "ISK", "THB", "IDR", "VND", "HRK", "MXN", "AED", "BTC", "ETH"),
            refreshHours = 8,
            staleTimeoutHours = 25,
            backFillInfo = RateApiBackFillInfo(
                url = "http://www.cbr.ru/scripts/XML_daily.asp?date_req=<date>",
                backFillLimit = LocalDate.of(2020, 1, 1),
                dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            ),
        ),
        "ECB" to RateApi(
            name = "ECB",
            base = Currencies["EUR"],
            aliases = arrayOf(),
            url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml",
            displayLink = "https://www.ecb.europa.eu/home/html/index.en.html",
            unsupported = setOf("BYN", "UAH", "KZT", "GEL", "VND", "MDL", "AMD", "RSD", "UZS", "KGS", "AED", "BTC", "ETH"),
            refreshHours = 8,
            staleTimeoutHours = 25,
            backFillInfo = RateApiBackFillInfo(
                // have to load list only last 90 days and iterate back
                url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml",
                backFillLimit = LocalDate.of(2022, 1, 1),
            ),
        ),
        "NBG" to RateApi(
            name = "NBG",
            base = Currencies["GEL"],
            aliases = arrayOf(),
            url = "https://nbg.gov.ge/gw/api/ct/monetarypolicy/currencies/en/json/",
            displayLink = "https://nbg.gov.ge/en/monetary-policy/currency",
            unsupported = setOf("BTC", "ETH", "HRK", "IDR", "MXN", "THB", "VND"),
            refreshHours = 8,
            staleTimeoutHours = 25,
            backFillInfo = RateApiBackFillInfo(
                // have to load list only last 90 days and iterate back
                url = "https://nbg.gov.ge/gw/api/ct/monetarypolicy/currencies/en/json/?date=<date>",
                backFillLimit = LocalDate.of(2023, 1, 1),
            ),
        ),
        "Fixer" to RateApi(
            name = "Fixer",
            base = Currencies["EUR"],
            aliases = arrayOf("Fixer", "fix", "фиксер", "фикс"),
            url = "http://data.fixer.io/api/latest?access_key=${System.getenv("FIXER_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,UZS,KGS,AUD,AED,BRL,RSD",
            displayLink = "https://fixer.io/",
            unsupported = setOf("ETH"),
            refreshHours = 8,
            backFillInfo = RateApiBackFillInfo(
                url = "http://data.fixer.io/api/<date>?access_key=${System.getenv("FIXER_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,UZS,KGS,AUD,AED,BRL,RSD",
                backFillLimit = LocalDate.of(2020, 1, 1),
            )
        ),
        "OpenExchangeRates" to RateApi(
            name = "OpenExchangeRates",
            base = Currencies["USD"],
            aliases = arrayOf("OER"),
            url = "https://openexchangerates.org/api/latest.json?app_id=${System.getenv("OPENEXCHANGERATES_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,UZS,KGS,AUD,AED,BRL,RSD",
            displayLink = "https://openexchangerates.org/",
            unsupported = setOf("ETH"),
            refreshHours = 1,
            backFillInfo = RateApiBackFillInfo(
                url = "https://openexchangerates.org/api/historical/<date>.json?app_id=${System.getenv("OPENEXCHANGERATES_ACCESS_KEY")}&symbols=BYN,USD,EUR,RUB,UAH,PLN,CZK,GBP,JPY,CNY,KZT,CHF,BGN,TRY,CAD,ISK,DKK,SEK,NOK,BTC,ILS,GEL,HUF,MXN,HRK,VND,KRW,MDL,HKD,RON,IDR,INR,THB,AMD,UZS,KGS,AUD,AED,BRL,RSD",
                backFillLimit = LocalDate.of(2020, 1, 1),
            ),
        ),
        "TraderMade" to RateApi(
            name = "TraderMade",
            base = Currencies["USD"],
            aliases = arrayOf("TM", "ТрейдерМейд"),
            url = "https://marketdata.tradermade.com/api/v1/live?api_key=${System.getenv("TRADERMADE_ACCESS_KEY")}&currency=USDEUR,USDRUB,USDPLN,USDCZK,USDGBP,USDJPY,USDCNY,USDCHF,USDTRY,USDCAD,USDISK,USDDKK,USDSEK,USDNOK,USDILS,USDBTC,USDETH,USDHUF,USDTHB,USDIDR,USDINR,USDRON,USDHKD,USDKRW,USDVND,USDHRK,USDMXN,USDAUD,USDAED,USDBRL",
            displayLink = "https://tradermade.com/",
            unsupported = setOf("BYN", "BGN", "GEL", "AMD", "RSD", "MDL", "KZT", "UZS", "KGS", "UAH"),
            refreshHours = 25,
            backFillInfo = RateApiBackFillInfo(
                url = "https://marketdata.tradermade.com/api/v1/historical?date=<date>&api_key=${System.getenv("TRADERMADE_ACCESS_KEY")}&currency=USDEUR,USDRUB,USDPLN,USDCZK,USDGBP,USDJPY,USDCNY,USDCHF,USDTRY,USDCAD,USDISK,USDDKK,USDSEK,USDNOK,USDILS,USDBTC,USDETH,USDHUF,USDTHB,USDIDR,USDINR,USDRON,USDHKD,USDKRW,USDVND,USDHRK,USDMXN,USDAUD,USDAED,USDBRL",
                backFillLimit = LocalDate.of(2022, 7, 1),
            ),
        ),
        "Forex" to RateApi(
            name = "Forex",
            base = Currencies["USD"],
            aliases = arrayOf("FX", "FC", "Форекс"),
            url = "https://fcsapi.com/api-v3/forex/latest?access_key=${System.getenv("FOREX_ACCESS_KEY")}&symbol=USD/BYN,USD/EUR,USD/RUB,USD/UAH,USD/PLN,USD/CZK,USD/GBP,USD/JPY,USD/CNY,USD/KZT,USD/CHF,USD/BGN,USD/TRY,USD/CAD,USD/ISK,USD/DKK,USD/SEK,USD/NOK,USD/ILS,USD/BTC,USD/ETH,USD/GEL,USD/HUF,USD/MXN,USD/HRK,USD/VND,USD/KRW,USD/MDL,USD/HKD,USD/RON,USD/IDR,USD/INR,USD/THB,USD/AMD,USD/UZS,USD/KGS,USD/AED,USD/BRL,USD/RSD",
            displayLink = "https://fcsapi.com/",
            unsupported = setOf(),
            refreshHours = 2,
            // backfill unsupported for Forex API
            backFillInfo = null

        ),
    )

    operator fun get(name: String): RateApi = ALL.getValue(name)
    operator fun contains(name: String) = name in ALL
    override fun iterator(): Iterator<RateApi> = ALL.values.iterator()
}
