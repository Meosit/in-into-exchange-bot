package org.mksn.inintobot.gcp.store

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.common.store.UserAggregateStatsStore

@Suppress("unused")
class FirestoreStoreProvider: StoreProvider {

    private val db: Firestore = FirestoreOptions.getDefaultInstance().toBuilder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build().service

    private val exchangeRateStore = FirestoreApiExchangeRateStore(db)
    private val userSettingsStore = FirestoreUserSettingsStore(db)

    override fun exchangeRateStore() = exchangeRateStore

    override fun userSettingsStore() = userSettingsStore

    override fun userAggregateStatsStore(): UserAggregateStatsStore {
        TODO("Not yet implemented")
    }
}