package com.groundhog.edgestore

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import java.util.UUID

/**
 * Simple file-backed store for the sample application. It keeps JSON for each collection
 * under a named store directory so multiple collections can share a single ObjectBox-equivalent store.
 */
class EdgeStore(private val context: Context, private val storeName: String) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val storeDir: File by lazy {
        File(context.filesDir, "edge-store/$storeName").apply { mkdirs() }
    }

    private fun collectionFile(collection: String): File = File(storeDir, "$collection.json")

    private fun <T> readCollection(serializer: KSerializer<T>, collection: String): MutableMap<String, T> {
        val file = collectionFile(collection)
        if (!file.exists()) return mutableMapOf()
        val raw = file.readText()
        if (raw.isBlank()) return mutableMapOf()
        val parsed = json.decodeFromString(MapSerializerAdapter(serializer), raw)
        return parsed.toMutableMap()
    }

    private fun <T> writeCollection(collection: String, content: Map<String, T>, serializer: KSerializer<T>) {
        val file = collectionFile(collection)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(MapSerializerAdapter(serializer), content))
    }

    @Synchronized
    fun <T : EdgeEntity> save(collection: String, entity: T, serializer: KSerializer<T>): T {
        if (entity.id.isBlank()) {
            entity.id = UUID.randomUUID().toString()
        }
        val existing = readCollection(serializer, collection)
        existing[entity.id] = entity
        writeCollection(collection, existing, serializer)
        return entity
    }

    @Synchronized
    fun <T : EdgeEntity> delete(collection: String, id: String, serializer: KSerializer<T>) {
        val existing = readCollection(serializer, collection)
        existing.remove(id)
        writeCollection(collection, existing, serializer)
    }

    @Synchronized
    fun <T : EdgeEntity> getAll(collection: String, serializer: KSerializer<T>): List<T> {
        val existing = readCollection(serializer, collection)
        return existing.values.toList()
    }

    @Synchronized
    fun <T : EdgeEntity> get(collection: String, id: String, serializer: KSerializer<T>): T? {
        val existing = readCollection(serializer, collection)
        return existing[id]
    }
}

private class MapSerializerAdapter<T>(private val valueSerializer: KSerializer<T>) :
    KSerializer<Map<String, T>> by kotlinx.serialization.builtins.MapSerializer(
        String.serializer(),
        valueSerializer
    )
