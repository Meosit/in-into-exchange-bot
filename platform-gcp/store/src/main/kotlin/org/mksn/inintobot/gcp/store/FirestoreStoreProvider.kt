package org.mksn.inintobot.gcp.store

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.mksn.inintobot.common.store.StoreProvider

@Suppress("unused")
class FirestoreStoreProvider: StoreProvider {

    private val db: Firestore = FirestoreOptions.getDefaultInstance().toBuilder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build().service

    private val exchangeRateStore = CachedFirestoreApiExchangeRateStore(FirestoreApiExchangeRateStore(db))
    private val userSettingsStore = CachedFirestoreUserSettingsStore(FirestoreUserSettingsStore(db))
    private val userAggregateStatsStore = FirestoreUserAggregateStatsStore(db)

    override fun exchangeRateStore() = exchangeRateStore

    override fun userSettingsStore() = userSettingsStore

    override fun userAggregateStatsStore() = userAggregateStatsStore
}
