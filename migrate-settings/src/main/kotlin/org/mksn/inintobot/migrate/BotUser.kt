package org.mksn.inintobot.migrate

import org.mksn.inintobot.common.user.UserSettings
import java.sql.Timestamp
import java.time.Instant


data class BotUser(
    val id: Long,
    val name: String,
    val lastUsed: Timestamp = Timestamp.from(Instant.now()),
    val lastQuery: String,
    val numRequests: Int = 1,
    val inlineRequests: Int = 0,
    val settings: UserSettings? = null
)
