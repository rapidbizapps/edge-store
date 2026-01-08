package edgestore.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.jvm.kotlin

/**
 * Pluggable serializer interface for EdgeStore payloads.
 */
interface Serializer {
    fun <T : Any> deserialize(payload: ByteArray, clazz: Class<T>): T
    fun serialize(entity: Any): ByteArray
}

/**
 * Default JSON serializer using kotlinx.serialization.
 */
class JsonSerializer : Serializer {
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserialize(payload: ByteArray, clazz: Class<T>): T {
        val jsonString = String(payload, Charsets.UTF_8)
        val kClass: KClass<T> = clazz.kotlin
        val kType: KType = kClass.createType()
        val serializer = serializer(kType) as KSerializer<T>
        return json.decodeFromString(serializer, jsonString)
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(entity: Any): ByteArray {
        val kType: KType = entity::class.createType()
        val serializer = serializer(kType) as KSerializer<Any>
        val jsonString = json.encodeToString(serializer, entity)
        return jsonString.toByteArray(Charsets.UTF_8)
    }
}
