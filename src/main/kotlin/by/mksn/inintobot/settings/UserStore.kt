package by.mksn.inintobot.settings

import by.mksn.inintobot.AppContext
import com.vladsch.kotlin.jdbc.Row
import com.vladsch.kotlin.jdbc.sqlQuery
import com.vladsch.kotlin.jdbc.usingDefault
import java.sql.Timestamp
import java.time.Instant

object UserStore {

    private const val CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL,
  name TEXT NOT NULL,
  last_used Timestamp,
  last_query TEXT NOT NULL,
  requests INTEGER NOT NULL,
  inline_requests INTEGER NOT NULL,
  settings TEXT NULL DEFAULT NULL,
  PRIMARY KEY (id)
)"""

    private const val UPSERT_SQL = """
INSERT INTO users (id, name, last_used, last_query, requests, inline_requests)
VALUES (:id, :name, :last_used, :last_query, :requests, :inline_requests)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name, last_used = EXCLUDED.last_used, last_query = EXCLUDED.last_query,
    requests = users.requests + EXCLUDED.requests, inline_requests = users.inline_requests + EXCLUDED.inline_requests
RETURNING id, name, last_used, last_query, requests, inline_requests, settings
"""

    private const val UPDATE_SETTINGS = """UPDATE users SET settings = ? WHERE id = ?"""


    fun initializeStore() {
        usingDefault { session ->
            session.execute(sqlQuery(CREATE_TABLE_SQL))
        }
    }

    fun refreshAndGet(id: Long, name: String, query: String, inlineQuery: Boolean): BotUser? = usingDefault { session ->
        val params = mapOf(
            "id" to id,
            "name" to name,
            "last_used" to Timestamp.from(Instant.now()),
            "last_query" to query,
            "requests" to 1,
            "inline_requests" to if (inlineQuery) 1 else 0
        )
        session.execute(sqlQuery(UPSERT_SQL, params)) { stmt ->
            stmt.resultSet.use { result ->
                if (result.next()) {
                    BotUser(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getTimestamp("last_used"),
                        result.getString("last_query"),
                        result.getInt("requests"),
                        result.getInt("inline_requests"),
                        result.getString("settings")?.let { AppContext.json.parse(UserSettings.serializer(), it) }
                    )
                } else {
                    null
                }
            }
        }
    }

    fun updateSettings(id: Long, settings: UserSettings) {
        val settingsString = AppContext.json.stringify(UserSettings.serializer(), settings)
        usingDefault { session -> session.update(sqlQuery(UPDATE_SETTINGS, settingsString, id)) }
    }

    fun lastUsed(limit: Int): List<BotUser> = usingDefault { session ->
        val query = """
            SELECT id, name, last_used, last_query, requests, inline_requests, settings 
            FROM users 
            ORDER BY last_used DESC LIMIT ?
        """.trimIndent()
        session.list(sqlQuery(query, limit)) { it.toBotUser() }
    }

    fun lastUsed(limit: Int, lastHours: Int): List<BotUser> = usingDefault { session ->
        val query = """
            SELECT id, name, last_used, last_query, requests, inline_requests, settings 
            FROM users 
            WHERE last_used > current_timestamp - interval '$lastHours hours'
            ORDER BY last_used DESC LIMIT ?
        """.trimIndent()
        session.list(sqlQuery(query, limit)) { it.toBotUser() }
    }

    fun userById(id: Long): BotUser? = usingDefault { session ->
        val query = """
            SELECT id, name, last_used, last_query, requests, inline_requests, settings 
            FROM users 
            WHERE id = ?
        """.trimIndent()
        session.list(sqlQuery(query, id)) { it.toBotUser() }.firstOrNull()
    }

    fun usersGenericSelect(afterFromClause: String): List<BotUser> = usingDefault { session ->
        val query = """
            SELECT id, name, last_used, last_query, requests, inline_requests, settings 
            FROM users 
            $afterFromClause
        """.trimIndent()
        session.list(sqlQuery(query)) { it.toBotUser() }
    }

    private fun Row.toBotUser() = BotUser(
        long("id"),
        string("name"),
        sqlTimestamp("last_used"),
        string("last_query"),
        int("requests"),
        int("inline_requests"),
        stringOrNull("settings")?.let { AppContext.json.parse(UserSettings.serializer(), it) }
    )

}