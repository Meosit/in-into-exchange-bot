package org.mksn.inintobot.gcp.store

import com.google.cloud.firestore.Firestore
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.store.UserSettingsStore
import org.mksn.inintobot.common.user.RateAlert
import org.mksn.inintobot.common.user.UserDefaultSettings
import org.mksn.inintobot.common.user.UserSettings

class FirestoreUserSettingsStore(private val db: Firestore) : UserSettingsStore {

    private val collectionName = "inintobot-user-settings"

    override fun save(id: String, settings: UserSettings) {
        db.collection(collectionName)
            .document(id)
            .set(settings.toFirestoreMap())
            .get()
    }

    override fun delete(id: String) {
        db.collection(collectionName)
            .document(id)
            .delete()
            .get()
    }

    override fun get(id: String): UserSettings? = db.collection(collectionName)
        .document(id).get().get().data?.fromFirestoreMap()

    override fun getAllWithAlerts(): Map<String, UserSettings> = db.collection(collectionName)
        .whereNotEqualTo("alerts", null)
        .get().get().documents
        .associate { doc -> doc.id to doc.data.fromFirestoreMap() }

    private fun UserSettings.toFirestoreMap(): Map<String, Any?> = mapOf(
        "language" to language,
        "decimalDigits" to decimalDigits,
        "defaultCurrency" to defaultCurrency,
        "apiName" to apiName,
        "outputCurrencies" to outputCurrencies,
        "dashboardCurrencies" to dashboardCurrencies,
        "hourlyRateUSD" to hourlyRateUSD?.toStr(),
        "thousandSeparator" to thousandSeparator?.toString(),
        "alerts" to (alerts?.map {
            mapOf(
                "id" to it.id,
                "apiName" to it.apiName,
                "fromCurrency" to it.fromCurrency,
                "toCurrency" to it.toCurrency,
                "isRelative" to it.isRelative,
                "value" to it.value.toStr(),
            )
        }),
    )

    private fun Map<String, Any>.fromFirestoreMap() = UserSettings(
        language = this["language"]?.toString() ?: UserDefaultSettings.LANGUAGE,
        decimalDigits = (this["decimalDigits"] as? Long)?.toInt() ?: UserDefaultSettings.DECIMAL_DIGITS,
        defaultCurrency = this["defaultCurrency"]?.toString() ?: UserDefaultSettings.DEFAULT_CURRENCY,
        apiName = this["apiName"]?.toString() ?: UserDefaultSettings.API_NAME,
        outputCurrencies = (this["outputCurrencies"] as? List<*>)?.map { it.toString() } ?: UserDefaultSettings.OUTPUT_CURRENCIES,
        dashboardCurrencies = (this["dashboardCurrencies"] as? List<*>)?.map { it.toString() } ?: UserDefaultSettings.DASHBOARD_CURRENCIES,
        hourlyRateUSD = this["hourlyRateUSD"]?.toString()?.toFixedScaleBigDecimal() ?: UserDefaultSettings.HOURLY_RATE_USD,
        thousandSeparator = this["thousandSeparator"]?.toString()?.firstOrNull() ?: UserDefaultSettings.THOUSAND_SEPARATOR,
        alerts = (this["alerts"] as? List<*>)?.mapNotNull { it as? Map<*, *> }?.map {
            RateAlert(
                id = it["id"].toString(),
                apiName = it["apiName"].toString(),
                fromCurrency = it["fromCurrency"].toString(),
                toCurrency = it["toCurrency"].toString(),
                isRelative = (it["isRelative"] as Boolean),
                value = (it["value"] as String).toFixedScaleBigDecimal(),
            )
        },
        persisted = true,
    )
}