package org.mksn.inintobot.gcp.store

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.rate.ApiExchangeRates
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import java.time.LocalDate
import java.time.LocalTime

class FirestoreApiExchangeRateStore(private val db: Firestore) : ApiExchangeRateStore {

    private val collectionName = "inintobot-exchange-rates"

    override fun historyStart(name: String): LocalDate = db.collection(collectionName)
        .orderBy("date", Query.Direction.ASCENDING)
        .whereEqualTo("api", name)
        .limit(1)
        .get().get().documents.firstOrNull()?.data?.fromFirestoreMap()?.date
        ?: LocalDate.now().plusDays(1)

    override fun save(rates: ApiExchangeRates) {
        val id = "${rates.api.name}-${rates.date}"
        db.collection(collectionName)
            .document(id)
            .set(rates.toFirestoreMap())
            .get()
    }

    override fun getForDate(name: String, date: LocalDate, backtrackDays: Int): ApiExchangeRates? {
        return with(db.collection(collectionName).whereEqualTo("api", name)) {
            if (backtrackDays != 0)
                whereLessThanOrEqualTo("date", date.toString())
                .whereGreaterThanOrEqualTo("date", date.minusDays(backtrackDays.toLong()).toString())
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
            else whereEqualTo("date", date.toString())
        }.get().get().documents.firstOrNull()?.data?.fromFirestoreMap()
    }

    override fun getHistoryForDate(name: String, date: LocalDate, backtrackDays: Int): List<ApiExchangeRates> {
        return with(db.collection(collectionName).whereEqualTo("api", name)) {
            if (backtrackDays != 0)
                whereLessThanOrEqualTo("date", date.toString())
                .whereGreaterThanOrEqualTo("date", date.minusDays(backtrackDays.toLong()).toString())
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(backtrackDays + 1)
            else whereEqualTo("date", date.toString())
        }.get().get().documents.map { it.data.fromFirestoreMap() }
    }

    override fun getLatest(name: String): ApiExchangeRates? = db.collection(collectionName)
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