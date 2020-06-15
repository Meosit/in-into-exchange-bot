package by.mksn.inintobot.misc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader


@Serializable
private data class BasicInfoEntity(
    val creatorId: String,
    val maxOutputLength: Int,
    val maxErrorLineLength: Int,
    val supportedLocales: Set<String>
)

/**
 * Various application system info
 */
object BasicInfo {

    private lateinit var info: BasicInfoEntity

    fun load(json: Json) {
        info = json.parse(BasicInfoEntity.serializer(), BasicInfo::class.java.classLoader
            .getResourceAsStream("basic-info.json")
            .let { it ?: throw IllegalStateException("Null resource stream") }
            .use { InputStreamReader(it).use(InputStreamReader::readText) })
    }


    val creatorId get() = info.creatorId
    val maxOutputLength get() = info.maxOutputLength
    val maxErrorLineLength get() = info.maxErrorLineLength

    val supportedLanguages get() = info.supportedLocales


}