package org.mksn.inintobot.migrate

import kotlinx.serialization.Serializable

@Serializable
data class MigrateInput(val herokuDbUrl: String)
