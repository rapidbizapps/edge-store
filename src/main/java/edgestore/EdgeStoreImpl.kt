package edgestore

import edgestore.util.EdgeLogger

/**
 * Internal implementation of EdgeStore.
 * Validates _id presence, enforces CRUD exclusively through this class,
 * uses EdgeBox for persistence, records EdgeDirty entries, and logs all operations.
 */
internal class EdgeStoreImpl(
    private val edgeBox: EdgeBox,
    private val config: EdgeStoreConfig
) : EdgeStore {

    override fun create(entity: EdgeEntity<*>, payload: ByteArray, ctx: EdgeContext): String {
        val deserializedEntity = config.serializer.deserialize(payload, entity.clazz)
        val _id = validateAndExtractId(deserializedEntity)
        EdgeLogger.logCreate(entity, _id, ctx)
        edgeBox.put(entity.name, _id, deserializedEntity)
        recordDirty(entity.name, _id, "CREATE", ctx)
        return _id
    }

    override fun update(entity: EdgeEntity<*>, _id: String, payload: ByteArray, ctx: EdgeContext) {
        val deserializedEntity = config.serializer.deserialize(payload, entity.clazz)
        validateId(deserializedEntity, _id)
        EdgeLogger.logUpdate(entity, _id, ctx)
        edgeBox.put(entity.name, _id, deserializedEntity)
        recordDirty(entity.name, _id, "UPDATE", ctx)
    }

    override fun delete(entity: EdgeEntity<*>, _id: String, ctx: EdgeContext) {
        EdgeLogger.logDelete(entity, _id, ctx)
        edgeBox.remove(entity.name, listOf(_id))
        recordDirty(entity.name, _id, "DELETE", ctx)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> query(entity: EdgeEntity<*>, filters: List<EdgeFilter>): List<T> {
        EdgeLogger.logQuery(entity, filters)
        return edgeBox.query(entity.name, entity.clazz, filters) as List<T>
    }

    override fun close() {
        edgeBox.close()
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
        val entityClass = entity.javaClass
        return try {
            entityClass.getDeclaredField("_id").apply { isAccessible = true }.get(entity) as String
        } catch (e: NoSuchFieldException) {
            throw IllegalArgumentException("Entity ${entityClass.simpleName} must have a '_id' property")
        }
    }

    private fun recordDirty(entityType: String, _id: String, operation: String, ctx: EdgeContext) {
        val dirty = EdgeDirty().apply {
            this.entityType = entityType
            this._id = _id
            this.operation = operation
            this.source = ctx.source
            this.actor = ctx.actor
            this.reason = ctx.reason
            this.timestamp = System.currentTimeMillis()
        }
        edgeBox.putDirty(dirty)
    }
}
