package org.mksn.inintobot.exchange.settings

interface UserSettingsStore {
    fun get(id: String): UserSettings?
    fun save(id: String, settings: UserSettings)
}