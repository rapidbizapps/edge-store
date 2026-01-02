package edgestore.util

import kotlinx.serialization.json.Json

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
        return json.decodeFromString(jsonString) as T
    }

    override fun serialize(entity: Any): ByteArray {
        val jsonString = json.encodeToString(entity)
        return jsonString.toByteArray(Charsets.UTF_8)
    }
}
