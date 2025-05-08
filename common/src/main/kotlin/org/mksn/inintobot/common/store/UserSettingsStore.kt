package org.mksn.inintobot.common.store

import org.mksn.inintobot.common.user.UserSettings

interface UserSettingsStore {
    fun get(id: String): UserSettings?

    fun getAllWithAlerts(): Map<String, UserSettings>

    fun save(id: String, settings: UserSettings)

    fun delete(id: String)
}