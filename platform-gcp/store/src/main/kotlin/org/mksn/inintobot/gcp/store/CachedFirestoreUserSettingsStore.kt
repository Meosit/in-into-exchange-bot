
package org.mksn.inintobot.gcp.store

import org.mksn.inintobot.common.store.UserSettingsStore
import org.mksn.inintobot.common.user.UserSettings
import java.time.LocalDateTime
import java.time.Duration

class CachedFirestoreUserSettingsStore(
    private val delegate: FirestoreUserSettingsStore,
    private val cacheSize: Int = 100,
    private val cacheTtl: Duration = Duration.ofHours(48)
) : UserSettingsStore {

    private val cache = LRUCache<String, CachedUserSettings>(cacheSize)
    private val alertsCache = LRUCache<String, CachedAlertsResult>(1) // Single entry for alerts query

    private data class CachedUserSettings(
        val settings: UserSettings?,
        val timestamp: LocalDateTime
    )

    private data class CachedAlertsResult(
        val alerts: Map<String, UserSettings>,
        val timestamp: LocalDateTime
    )

    override fun save(id: String, settings: UserSettings) {
        delegate.save(id, settings)
        cache.put(id, CachedUserSettings(settings, LocalDateTime.now()))
        alertsCache.clear() // Clear alerts cache when any user settings change
    }

    override fun delete(id: String) {
        delegate.delete(id)
        cache.remove(id)
        alertsCache.clear() // Clear alerts cache when any user settings change
    }

    override fun get(id: String): UserSettings? {
        cache.get(id)?.let { cached ->
            if (Duration.between(cached.timestamp, LocalDateTime.now()) < cacheTtl) {
                return cached.settings
            }
        }

        val settings = delegate.get(id)
        cache.put(id, CachedUserSettings(settings, LocalDateTime.now()))
        return settings
    }

    override fun getAllWithAlerts(): Map<String, UserSettings> {
        alertsCache.get("all")?.let { cached ->
            if (Duration.between(cached.timestamp, LocalDateTime.now()) < cacheTtl) {
                return cached.alerts
            }
        }

        val alerts = delegate.getAllWithAlerts()
        alertsCache.put("all", CachedAlertsResult(alerts, LocalDateTime.now()))
        return alerts
    }
}
