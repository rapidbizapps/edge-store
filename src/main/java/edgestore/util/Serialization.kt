package edgestore.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Pluggable serializer interface for EdgeStore payloads.
 */
interface Serializer {
    fun <T> deserialize(payload: ByteArray, clazz: Class<T>): T
    fun serialize(entity: Any): ByteArray
}

/**
 * Default JSON serializer using kotlinx.serialization.
 */
class JsonSerializer : Serializer {
    private val json = Json { ignoreUnknownKeys = true }

    override fun <T> deserialize(payload: ByteArray, clazz: Class<T>): T {
        val jsonString = String(payload, Charsets.UTF_8)
        return json.decodeFromString(serializer(clazz.kotlin), jsonString) as T
    }

    override fun serialize(entity: Any): ByteArray {
        val jsonString = json.encodeToString(serializer(entity::class), entity)
        return jsonString.toByteArray(Charsets.UTF_8)
    }
}
