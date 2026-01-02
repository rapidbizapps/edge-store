package edgestore

import edgestore.util.EdgeLogger
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Internal implementation of EdgeStore.
 * Validates _id presence, enforces CRUD exclusively through this class,
 * uses EdgeBox for persistence, records EdgeDirty entries, and logs all operations.
 */
internal class EdgeStoreImpl(
    private val edgeBox: EdgeBox,
    private val config: EdgeStoreConfig
) : EdgeStore {

    override fun create(entity: EdgeEntity, payload: ByteArray, ctx: EdgeContext): String {
        val entityClass = Class.forName(entity.name)
        val deserializedEntity = config.serializer.deserialize(payload, entityClass)
        val _id = validateAndExtractId(deserializedEntity)
        EdgeLogger.logCreate(entity, _id, ctx)
        edgeBox.put(deserializedEntity)
        recordDirty(entity.name, _id, "CREATE", ctx)
        return _id
    }

    override fun update(entity: EdgeEntity, _id: String, payload: ByteArray, ctx: EdgeContext) {
        val entityClass = Class.forName(entity.name)
        val deserializedEntity = config.serializer.deserialize(payload, entityClass)
        validateId(deserializedEntity, _id)
        EdgeLogger.logUpdate(entity, _id, ctx)
        edgeBox.put(deserializedEntity)
        recordDirty(entity.name, _id, "UPDATE", ctx)
    }

    override fun delete(entity: EdgeEntity, _id: String, ctx: EdgeContext) {
        val entityClass = Class.forName(entity.name)
        EdgeLogger.logDelete(entity, _id, ctx)
        edgeBox.remove(entityClass, _id)
        recordDirty(entity.name, _id, "DELETE", ctx)
    }

    override fun <T : Any> query(entity: EdgeEntity, filters: List<EdgeFilter>): List<T> {
        val entityClass = Class.forName(entity.name) as Class<T>
        EdgeLogger.logQuery(entity, filters)
        val mapFilters = filters.associate { it.field to it.value }
        return edgeBox.query(entityClass, mapFilters)
    }

    private fun validateAndExtractId(entity: Any): String {
        val _id = extractId(entity)
        if (_id.isBlank()) {
            throw IllegalArgumentException("Entity must have a non-blank '_id' field")
        }
        return _id
    }

    private fun validateId(entity: Any, expectedId: String) {
        val actualId = extractId(entity)
        if (actualId != expectedId) {
            throw IllegalArgumentException("_id mismatch: expected $expectedId, got $actualId")
        }
    }

    private fun extractId(entity: Any): String {
        val kClass = entity::class
        val idProperty = kClass.memberProperties.find { it.name == "_id" }
            ?: throw IllegalArgumentException("Entity ${kClass.simpleName} must have a '_id' property")
        return (idProperty as KProperty1<Any, *>).get(entity) as String
    }

    private fun recordDirty(entityType: String, _id: String, operation: String, ctx: EdgeContext) {
        val dirty = EdgeDirty().apply {
            this.entityType = entityType
            this._id = _id
            this.timestamp = System.currentTimeMillis()
        }
        edgeBox.put(dirty)
    }
}
