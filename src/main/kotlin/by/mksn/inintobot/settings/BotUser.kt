package by.mksn.inintobot.settings

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