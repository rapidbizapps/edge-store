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

    override fun create(entityClass: Class<*>, payload: ByteArray): String {
        val entity = config.serializer.deserialize(payload, entityClass)
        val _id = validateAndExtractId(entity)
        EdgeLogger.logCreate(entityClass, _id)
        edgeBox.put(entity)
        recordDirty(entityClass.simpleName, _id, "CREATE")
        return _id
    }

    override fun update(entityClass: Class<*>, _id: String, payload: ByteArray) {
        val entity = config.serializer.deserialize(payload, entityClass)
        validateId(entity, _id)
        EdgeLogger.logUpdate(entityClass, _id)
        edgeBox.put(entity)
        recordDirty(entityClass.simpleName, _id, "UPDATE")
    }

    override fun delete(entityClass: Class<*>, _id: String) {
        EdgeLogger.logDelete(entityClass, _id)
        edgeBox.remove(entityClass, _id)
        recordDirty(entityClass.simpleName, _id, "DELETE")
    }

    override fun <T : Any> query(entityClass: Class<T>, filters: Map<String, Any>): List<T> {
        EdgeLogger.logQuery(entityClass, filters)
        return edgeBox.query(entityClass, filters)
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

    private fun recordDirty(entityType: String, _id: String, operation: String) {
        val dirty = EdgeDirty().apply {
            this.entityType = entityType
            this._id = _id
            this.timestamp = System.currentTimeMillis()
        }
        edgeBox.put(dirty)
    }
}
