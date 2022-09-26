package org.mksn.inintobot.gcp.store

import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Precondition
import com.google.cloud.firestore.SetOptions
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.EvaluatedExpression
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.store.UserAggregateStatsStore
import org.mksn.inintobot.common.user.UserAggregateStats
import org.mksn.inintobot.common.user.UserSettings
import java.util.logging.Logger


class FirestoreUserAggregateStatsStore(private val db: Firestore) : UserAggregateStatsStore {


    private val logger = Logger.getLogger(FirestoreUserAggregateStatsStore::class.simpleName)
    private val collectionName = "inintobot-user-stats"
    private val objectId = "AGGREGATED_STATS"

    init {
        if (!db.collection(collectionName).document(objectId).get().get().exists()) {
            db.collection(collectionName).document(objectId).set(mapOf(), SetOptions.merge())
        }
    }

    override fun get(): UserAggregateStats {
        val data: Map<String, Any> = db.collection(collectionName).document(objectId).get().get().data
            ?: throw Exception("No object under $collectionName.$objectId exists")

        return UserAggregateStats(
            totalRequests = data["totalRequests"] as? Long ?: 0L,
            totalRequestsWithHistory = data["totalRequestsWithHistory"] as? Long ?: 0L,
            inlineRequests = data["inlineRequests"] as? Long ?: 0L,
            inlineRequestsWithHistory = data["inlineRequestsWithHistory"] as? Long ?: 0L,
            requestsRateApiUsage = data["requestsRateApiUsage"].toMap(RateApis::get),
            requestsBaseCurrencyUsage = data["requestsBaseCurrencyUsage"].toMap(Currencies::get),
            requestsInvolvedCurrencyUsage = data["requestsInvolvedCurrencyUsage"].toMap(Currencies::get),
            requestsOutputCurrencyUsage = data["requestsOutputCurrencyUsage"].toMap(Currencies::get),
            expressionTypeUsage = data["expressionTypeUsage"].toMap(ExpressionType::valueOf),

            totalRequestsErrors = data["totalRequestsErrors"] as? Long ?: 0L,
            inlineRequestsErrors = data["inlineRequestsErrors"] as? Long ?: 0L,
            errorUsage = data["errorUsage"].toMap { it },

            botCommandUsage = data["botCommandUsage"].toMap { it },

            usersWithCustomizedSettings = data["usersWithCustomizedSettings"] as? Long ?: 0L,
            settingsDefaultCurrencyUsage = data["settingsDefaultCurrencyUsage"].toMap(Currencies::get),
            settingsDefaultRateApiUsage = data["settingsDefaultRateApiUsage"].toMap(RateApis::get),
            settingsOutputCurrencyUsage = data["settingsOutputCurrencyUsage"].toMap(Currencies::get),
            settingsLanguageUsage = data["settingsLanguageUsage"].toMap { it },
        )
    }

    override fun logExchangeRequestUsage(
        expression: EvaluatedExpression,
        rateApi: RateApi,
        outputCurrencies: List<Currency>,
        inlineRequest: Boolean,
        historyRequest: Boolean
    ) = runCatching<Unit> {
        db.collection(collectionName).document(objectId).update(sequence {
            yield("totalRequests" to FieldValue.increment(1))
            yield("totalRequestsWithHistory" to FieldValue.increment(historyRequest.toIncrement()))
            yield("inlineRequests" to FieldValue.increment(inlineRequest.toIncrement()))
            yield("inlineRequestsWithHistory" to FieldValue.increment((inlineRequest && historyRequest).toIncrement()))
            yield("requestsRateApiUsage.${rateApi.name}" to FieldValue.increment(1))
            yield("requestsBaseCurrencyUsage.${expression.baseCurrency.code}" to FieldValue.increment(1))
            expression.involvedCurrencies.filterNot { it.code == expression.baseCurrency.code }.forEach { c ->
                yield("requestsInvolvedCurrencyUsage.${c.code}" to FieldValue.increment(1))
            }
            outputCurrencies.forEach { c ->
                yield("requestsOutputCurrencyUsage.${c.code}" to FieldValue.increment(1))
            }
            yield("expressionTypeUsage.${expression.type.name}" to FieldValue.increment(1))
        }.toMap(), Precondition.NONE)
    }.onFailure { logger.warning("$expression, ${rateApi.name}, $outputCurrencies, history: $historyRequest, inline: $inlineRequest. ${it.stackTraceToString()}") }

    override fun logExchangeErrorRequest(errorType: String, inlineRequest: Boolean) = runCatching<Unit> {
        db.collection(collectionName).document(objectId).update(
            mapOf(
                "totalRequestsErrors" to FieldValue.increment(1),
                "inlineRequestsErrors" to FieldValue.increment(inlineRequest.toIncrement()),
                "errorUsage.$errorType" to FieldValue.increment(1)
            ), Precondition.NONE
        )
    }.onFailure { logger.warning("Error Type: $errorType, inline: $inlineRequest. ${it.stackTraceToString()}") }

    override fun logSettingsChange(old: UserSettings, new: UserSettings) = runCatching {
        val value = sequence {
            val dec = if (old.containDefaultsOnly()) 0L else -1L
            if (new.defaultCurrency != old.defaultCurrency) {
                yield("settingsDefaultCurrencyUsage.${new.defaultCurrency}" to FieldValue.increment(1))
                yield("settingsDefaultCurrencyUsage.${old.defaultCurrency}" to FieldValue.increment(dec))
            }
            if (new.apiName != old.apiName) {
                yield("settingsDefaultRateApiUsage.${new.apiName}" to FieldValue.increment(1))
                yield("settingsDefaultRateApiUsage.${old.apiName}" to FieldValue.increment(dec))
            }
            if (new.language != old.language) {
                yield("settingsLanguageUsage.${new.language}" to FieldValue.increment(1))
                yield("settingsLanguageUsage.${old.language}" to FieldValue.increment(dec))
            }
            (old.outputCurrencies - new.outputCurrencies).forEach {
                yield("settingsOutputCurrencyUsage.$it" to FieldValue.increment(dec))
            }
            (new.outputCurrencies - old.outputCurrencies).forEach {
                yield("settingsOutputCurrencyUsage.$it" to FieldValue.increment(1))
            }
            when {
                old.containDefaultsOnly() && !new.containDefaultsOnly() ->
                    yield("usersWithCustomizedSettings" to FieldValue.increment(1))

                !old.containDefaultsOnly() && new.containDefaultsOnly() ->
                    yield("usersWithCustomizedSettings" to FieldValue.increment(-1))
            }
        }.toMap()
        if (value.isNotEmpty()) {
            db.collection(collectionName).document(objectId).update(value, Precondition.NONE)
        }
    }.onFailure { logger.warning("old: $old, new: $new. ${it.stackTraceToString()}") }

    override fun logBotCommandUsage(command: String) = runCatching<Unit> {
        db.collection(collectionName).document(objectId).update(
            mapOf("botCommandUsage.${command.replace("/", "")}" to FieldValue.increment(1)),
            Precondition.NONE
        )
    }.onFailure { logger.warning("command: $command. ${it.stackTraceToString()}") }

    private fun Boolean.toIncrement() = this.compareTo(false).toLong()

    private inline fun <R> Any?.toMap(get: (String) -> R): Map<R, Long> = (this as? Map<*, *>)
            ?.map { (key, value) -> get(key.toString()) to (value as? Long ?: 0L) }
            ?.toMap() ?: mapOf()


}
