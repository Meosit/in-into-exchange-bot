package org.mksn.inintobot.exchange.settings

import com.google.cloud.firestore.Firestore
import org.mksn.inintobot.misc.FirestoreHolder

class FirestoreUserSettingsStore(private val db: Firestore = FirestoreHolder.INSTANCE) : UserSettingsStore {

    private val collectionSuffix = "inintobot-user-settings"

    override fun save(id: String, settings: UserSettings) {
        db.collection(collectionSuffix)
            .document(id)
            .set(settings.toFirestoreMap())
            .get()
    }

    override fun get(id: String): UserSettings? = db.collection(collectionSuffix)
        .document("").get().get().data?.fromFirestoreMap()

    private fun UserSettings.toFirestoreMap(): Map<String, Any> = mapOf(
        "language" to language,
        "decimalDigits" to decimalDigits,
        "defaultCurrency" to defaultCurrency,
        "apiName" to apiName,
        "outputCurrencies" to outputCurrencies,
        "dashboardCurrencies" to dashboardCurrencies,
        "dashboardOutputAll" to dashboardOutputAll,
    )

    private fun Map<String, Any>.fromFirestoreMap() = UserSettings(
        language = this["language"].toString(),
        decimalDigits = this["decimalDigits"] as Int,
        defaultCurrency = this["defaultCurrency"].toString(),
        apiName = this["apiName"].toString(),
        outputCurrencies = (this["outputCurrencies"] as List<*>).map { it.toString() },
        dashboardCurrencies = (this["dashboardCurrencies"] as List<*>).map { it.toString() },
        dashboardOutputAll = this["dashboardOutputAll"] as Boolean,
    )
}