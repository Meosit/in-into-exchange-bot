package org.mksn.inintobot.migrate

import com.vladsch.kotlin.jdbc.Row
import com.vladsch.kotlin.jdbc.SessionImpl
import com.vladsch.kotlin.jdbc.sqlCall
import com.vladsch.kotlin.jdbc.usingDefault
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.mksn.inintobot.common.HttpBotFunction
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.common.user.UserSettings
import java.io.InputStream
import java.net.URI
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger(MigrateHerokuFunction::class.simpleName)

@Suppress("unused")
class MigrateHerokuFunction : HttpBotFunction {

    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun Row.toBotUser() = BotUser(
        long("id"),
        string("name"),
        sqlTimestamp("last_used"),
        string("last_query"),
        int("requests"),
        int("inline_requests"),
        stringOrNull("settings")?.let { json.decodeFromString(UserSettings.serializer(), it) }
    )

    private fun initializeDataSource(dbUrl: String) {
        val dbUri = URI(dbUrl)
        val (username: String, password: String) = dbUri.userInfo.split(":")
        val jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=require"
        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl
        config.username = username
        config.password = password
        config.addDataSourceProperty("maximumPoolSize", 1)
        val ds = HikariDataSource(config)
        SessionImpl.defaultDataSource = { ds }
        logger.info("JDBC url: $jdbcUrl")
        logger.info("Initialized UserStore")
    }


    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun serve(input: InputStream): Int {
        val inputObject = json.decodeFromStream<MigrateInput>(input)
        val storeProvider = StoreProvider.load()
        val settingsStore = storeProvider.userSettingsStore()
        val statsStore = storeProvider.userAggregateStatsStore()

        initializeDataSource(inputObject.herokuDbUrl)
        usingDefault { session ->
            var offset = 0
            val limit = 100
            var i = 1
            do {
                val users = session.list(sqlCall("SELECT id, name, last_used, last_query, requests, inline_requests, settings FROM users ORDER BY id LIMIT $limit OFFSET $offset")) { it.toBotUser() }
                users.forEach {
                    logger.info("${i} Migrating user ${it.name} (${it.id}: Requests: ${it.numRequests} (${it.inlineRequests}), Settings: ${it.settings != null}, Used: ${it.lastUsed}")
                    if (it.settings != null) {
                        settingsStore.save(it.id.toString(), it.settings)
                        statsStore.logSettingsChange(UserSettings(persisted = false), it.settings)
                    }
                    statsStore.logMigratedTotalUsage(it.numRequests.toLong(), it.inlineRequests.toLong())
                    i++
                }
                offset += users.size
                logger.info("New offset is $offset")
            } while (users.size == limit)
        }
        return HttpStatusCode.OK.value
    }
}