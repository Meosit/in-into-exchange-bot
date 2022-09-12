package org.mksn.inintobot.rates.store

import com.google.cloud.firestore.FieldPath
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.misc.FirestoreHolder
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.misc.toStr
import org.mksn.inintobot.rates.ApiExchangeRates
import org.mksn.inintobot.rates.RateApis
import java.time.LocalDate
import java.time.LocalTime

class FirestoreApiExchangeRateStore(private val db: Firestore = FirestoreHolder.INSTANCE) : ApiExchangeRateStore {

    private val collectionSuffix = "inintobot-exchange-rates"

    override fun save(rates: ApiExchangeRates) {
        val id = "${rates.api.name}-${rates.date}"
        db.collection(collectionSuffix)
            .document(id)
            .set(rates.toFirestoreMap())
            .get()
    }

    override fun getForDate(name: String, date: LocalDate, backtrackDays: Int): ApiExchangeRates? {
        return db.collection(collectionSuffix)
            .whereEqualTo("api", name)
            .whereLessThanOrEqualTo(FieldPath.documentId(), date.toString())
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), date.minusDays(backtrackDays.toLong()).toString())
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .get()
            .documents
            .firstOrNull()?.data?.fromFirestoreMap()
    }

    override fun getLatest(name: String): ApiExchangeRates? = db.collection(collectionSuffix)
        .whereEqualTo("api", name)
        .orderBy("date", Query.Direction.DESCENDING)
        .limit(1)
        .get()
        .get().documents.firstOrNull()?.data?.fromFirestoreMap()

    private fun ApiExchangeRates.toFirestoreMap(): Map<String, Any> = mapOf(
        "time" to time.toString(),
        "date" to date.toString(),
        "api" to api.name,
        "rates" to rates.map { (key, value) -> key.code to value.toStr() }.toMap()
    )

    private fun Map<String, Any>.fromFirestoreMap() = ApiExchangeRates(
        time = LocalTime.parse(this["time"].toString()),
        date = LocalDate.parse(this["date"].toString()),
        api = RateApis[(this["api"].toString())],
        rates = (this["rates"] as Map<*, *>)
            .map { (key, value) -> Currencies[key.toString()] to value.toString().toFixedScaleBigDecimal() }
            .toMap()
    )
}