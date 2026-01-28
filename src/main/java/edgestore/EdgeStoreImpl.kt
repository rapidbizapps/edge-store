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

    // =========================================================================
    // Direct Entity Mode Implementation
    // =========================================================================

    override fun <T : Any> findAllDirect(clazz: Class<T>): List<T> {
        return edgeBox.boxFor(clazz).all
    }

    override fun <T : Any> findByIdDirect(clazz: Class<T>, id: Long): T? {
        return edgeBox.boxFor(clazz).get(id)
    }

    override fun <T : Any> findByBusinessIdDirect(clazz: Class<T>, _id: String): T? {
        // Query by _id field (from BaseModel)
        val box = edgeBox.boxFor(clazz)
        // Use reflection to find the _id property
        return box.all.find { entity ->
            try {
                val field = entity.javaClass.getDeclaredField("_id")
                field.isAccessible = true
                field.get(entity) == _id
            } catch (e: Exception) {
                // Try superclass (BaseModel)
                try {
                    val field = entity.javaClass.superclass?.getDeclaredField("_id")
                    field?.isAccessible = true
                    field?.get(entity) == _id
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    override fun <T : Any> putDirect(entity: T): Long {
        @Suppress("UNCHECKED_CAST")
        val box = edgeBox.boxFor(entity.javaClass as Class<T>)
        return box.put(entity)
    }

    override fun <T : Any> putAllDirect(entities: List<T>) {
        if (entities.isEmpty()) return
        @Suppress("UNCHECKED_CAST")
        val box = edgeBox.boxFor(entities.first().javaClass as Class<T>)
        box.put(entities)
    }

    override fun <T : Any> removeDirect(clazz: Class<T>, id: Long) {
        edgeBox.boxFor(clazz).remove(id)
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

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
