package org.mksn.inintobot.rates

import org.mksn.inintobot.currency.Currency
import java.time.LocalDate


class NotFoundException(override val cause: Throwable? = null) : RuntimeException("Failed to access underlying store")
class StoreUnavailableException(override val cause: Throwable? = null) : RuntimeException("Failed to access underlying store")
class UnknownCurrencyException(val api: RateApi, val currency: Currency) : RuntimeException("Currency ${currency.code} not found for the API ${api.name}")
class MissingCurrenciesException(val api: RateApi, val exchanges: List<Exchange>, val missing: List<Currency>) : RuntimeException("Some currency rates are missing in the API ${api.name}: ${missing.joinToString { it.code }}")