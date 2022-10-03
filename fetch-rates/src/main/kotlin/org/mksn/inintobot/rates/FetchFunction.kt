package org.mksn.inintobot.rates

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.HttpBotFunction
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.rate.ApiExchangeRates
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.rate.RateApiBackFillInfo
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.rates.fetch.ApiRateFetcher
import java.io.InputStream
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.logging.Logger

private const val numRetries = 3
private val logger: Logger = Logger.getLogger(FetchFunction::class.simpleName)

@Suppress("unused")
class FetchFunction(
    private val storeProvider: StoreProvider = StoreProvider.load(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    private val httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(json)
        }
    }
) : HttpBotFunction {

    init {
//        System.getenv("FOREX_ACCESS_KEY").ifEmpty { throw Exception("FOREX_ACCESS_KEY env must be provided") }
//        System.getenv("FIXER_ACCESS_KEY").ifEmpty { throw Exception("FIXER_ACCESS_KEY env must be provided") }
//        System.getenv("TRADERMADE_ACCESS_KEY").ifEmpty { throw Exception("TRADERMADE_ACCESS_KEY env must be provided") }
//        System.getenv("OPENEXCHANGERATES_ACCESS_KEY").ifEmpty { throw Exception("OPENEXCHANGERATES_ACCESS_KEY env must be provided") }
    }

    override suspend fun serve(input: InputStream): Int {
        val jsonBody = input.reader().readText()
        val request = if (jsonBody.isNotBlank()) {
            json.decodeFromString(FetchRequest.serializer(), jsonBody)
        } else FetchRequest()
        logger.info(request.toString())
        val jobs = RateApis
            .filterNot { it.name in request.skipApis }
            .map {
                it to CoroutineScope(Dispatchers.Default).async {
                    reloadOne(request.backfill, request.date, it, storeProvider.exchangeRateStore(), httpClient, json)
                }
            }

        val errors = jobs.mapNotNull { (api, job) -> job.await()?.let { api to it }  }
            .associateBy({ it.first }, { it.second })
        if (errors.isNotEmpty()) {
            throw Exception("Failed to load rates for ${errors.keys.joinToString { it.name }}:\n${errors.entries.joinToString("\n") { (k, v) -> "${k.name} -> ${v.message}" }}")
        }
        return HttpStatusCode.OK.value
    }

    private suspend fun reloadOne(
        backfill: Boolean,
        onDate: LocalDate?,
        api: RateApi,
        store: ApiExchangeRateStore,
        httpClient: HttpClient,
        json: Json
    ): Exception? {
        val hour = ZonedDateTime.now(ZoneOffset.UTC).hour
        val fetcher = ApiRateFetcher.forApi(api, httpClient, json)
        var error: Exception? = null
        val backFillInfo = api.backFillInfo
        when {
            backfill ->
                runBackfill(backFillInfo, store, api, fetcher)
            ((hour + 1) % api.refreshHours == 0) || onDate != null ->
                error = runRegularRefresh(api, onDate, store, backFillInfo, fetcher)
            else ->
                logger.info("${api.name}: Skipping load as current hour $hour not aligned with refresh rate ${api.refreshHours}")
        }
        return error
    }

    private suspend fun runRegularRefresh(
        api: RateApi,
        onDate: LocalDate?,
        store: ApiExchangeRateStore,
        backFillInfo: RateApiBackFillInfo?,
        fetcher: ApiRateFetcher
    ): Exception? {
        var error: Exception? = null
        for (i in 1..numRetries) {
            logger.info("${api.name}: Reloading exchange rates ${onDate ?: ""} (try $i)...")
            try {
                logger.info("${api.name}: Loaded rates  ${onDate ?: ""}")
                val oldRates = if (onDate == null) {
                    store.runCatching { getLatest(api.name) }.onFailure {
                        logger.severe("${api.name}: Failed fetch latest rates for api: $it")
                    }.getOrNull()
                } else null
                val now = LocalDate.now()
                val rates =
                    if (backFillInfo != null && onDate == null && oldRates != null && oldRates.date != now.minusDays(1)) {
                        logger.warning(
                            "${api.name} Found gap in rates: latest ${oldRates.date} is ${
                                ChronoUnit.DAYS.between(
                                    oldRates.date,
                                    now
                                )
                            } behind from now $now"
                        )
                        fetcher.fetch(Currencies, oldRates.date.plusDays(1))
                    } else fetcher.fetch(Currencies, onDate)
                val refreshed = onDate
                    ?.let { ZonedDateTime.of(LocalDateTime.of(it, LocalTime.of(23, 59, 59)), ZoneOffset.UTC) }
                    ?: ZonedDateTime.now(ZoneOffset.UTC).withNano(0)
                if (oldRates != null && oldRates.date == refreshed.toLocalDate() && oldRates.rates == rates) {
                    logger.info("${api.name}: Rates were not updated since last check on ${oldRates.date} ${oldRates.time}")
                } else {
                    store.save(ApiExchangeRates(refreshed.toLocalTime(), refreshed.toLocalDate(), api, rates))
                    logger.info("${api.name}: Stored rates for ${refreshed.toLocalDate()} ${refreshed.toLocalTime()} ")
                }
                break
            } catch (e: Exception) {
                logger.severe("${api.name}: Failed to load or store new rates: \n${e.stackTraceToString()}")
                error = e
            }
        }
        return error
    }

    private suspend fun runBackfill(
        backFillInfo: RateApiBackFillInfo?,
        store: ApiExchangeRateStore,
        api: RateApi,
        fetcher: ApiRateFetcher
    ) {
        if (backFillInfo != null) {
            val date = store.historyStart(api.name).minusDays(1)
            val days = ChronoUnit.DAYS.between(backFillInfo.backFillLimit, date).toInt()
            if (!backFillInfo.backFillDisabled && days >= 0) {
                generateSequence(date) { it.minusDays(1) }.take(days + 1).take(30).forEach {
                    runCatching {
                        logger.info("${api.name}: Loading historical data for $it")
                        val rates = fetcher.fetch(Currencies, it)
                        store.save(ApiExchangeRates(LocalTime.of(23, 59, 59), it, api, rates))
                        logger.info("${api.name}: Loaded historical rates for $it")
                    }.onFailure { logger.severe("${api.name}: Failed to load or store historical rates: \n${it.stackTraceToString()}") }
                }
            } else {
                logger.info("${api.name}: Skipping backfill: ${backFillInfo.backFillLimit} > $date or disabled: ${backFillInfo.backFillDisabled}")
            }
        } else {
            logger.info("${api.name}: Skipping backfill as it's not supported")
        }
    }
}