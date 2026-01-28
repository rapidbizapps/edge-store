package edgestore

import edgestore.util.Serializer
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder

/**
 * Internal ObjectBox wrapper that supports two modes:
 *
 * 1. **Direct entity mode**: User entities extend BaseModel and are actual ObjectBox entities.
 *    Use boxFor<T>() to get a Box for direct CRUD operations.
 *
 * 2. **JSON serialization mode**: User entities are stored as JSON in EdgeRecord.
 *    Use put(), remove(), query() methods for serialized storage.
 */
internal class EdgeBox(
    private val boxStore: BoxStore,
    private val serializer: Serializer
) {

    private val recordBox: Box<EdgeRecord>
        get() = boxStore.boxFor(EdgeRecord::class.java)

    private val dirtyBox: Box<EdgeDirty>
        get() = boxStore.boxFor(EdgeDirty::class.java)

    /**
     * Returns a Box for direct entity operations.
     * Use this when entities extend BaseModel and are actual ObjectBox entities.
     *
     * @param clazz The entity class
     * @return A Box for the entity type
     */
    fun <T : Any> boxFor(clazz: Class<T>): Box<T> {
        return boxStore.boxFor(clazz)
    }

    /**
     * Puts an entity into the store by serializing it to an EdgeRecord.
     * If an entity with the same _id and entityType exists, it is replaced.
     *
     * @param entityType The entity type name
     * @param _id The business identifier
     * @param entity The entity to store
     * @return The stored EdgeRecord's ObjectBox ID
     */
    fun put(entityType: String, _id: String, entity: Any): Long {
        val payload = String(serializer.serialize(entity), Charsets.UTF_8)
        val now = System.currentTimeMillis()

        // Check if record exists
        val existing = findRecord(entityType, _id)

        val record = existing ?: EdgeRecord().apply {
            this._id = _id
            this.entityType = entityType
            this.createdAt = now
        }

        record.payload = payload
        record.updatedAt = now

        return recordBox.put(record)
    }

    /**
     * Removes entities by their business _id.
     */
    fun remove(entityType: String, _ids: List<String>) {
        if (_ids.isEmpty()) return

        val query = recordBox.query()
            .equal(EdgeRecord_.entityType, entityType, QueryBuilder.StringOrder.CASE_SENSITIVE)

        if (_ids.size == 1) {
            query.equal(EdgeRecord_._id, _ids[0], QueryBuilder.StringOrder.CASE_SENSITIVE)
        } else {
            query.`in`(EdgeRecord_._id, _ids.toTypedArray(), QueryBuilder.StringOrder.CASE_SENSITIVE)
        }

        val records = query.build().find()
        if (records.isNotEmpty()) {
            recordBox.remove(records)
        }
        query.close()
    }

    /**
     * Queries entities based on structured filters.
     * Filters by entityType first, then applies additional filters in memory.
     */
    fun <T : Any> query(entityType: String, clazz: Class<T>, filters: List<EdgeFilter>): List<T> {
        // Get all records of this entity type
        val query = recordBox.query()
            .equal(EdgeRecord_.entityType, entityType, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()

        val records = query.find()
        query.close()

        // Deserialize and filter in memory
        val entities = records.mapNotNull { record ->
            try {
                serializer.deserialize(record.payload.toByteArray(Charsets.UTF_8), clazz)
            } catch (e: Exception) {
                null
            }
        }

        // Apply filters in memory
        return if (filters.isEmpty()) {
            entities
        } else {
            entities.filter { entity -> matchesFilters(entity, filters) }
        }
    }

    /**
     * Retrieves an entity by its business _id.
     */
    fun <T : Any> getById(entityType: String, clazz: Class<T>, _id: String): T? {
        val record = findRecord(entityType, _id) ?: return null
        return try {
            serializer.deserialize(record.payload.toByteArray(Charsets.UTF_8), clazz)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Puts a dirty record for mutation tracking.
     */
    fun putDirty(dirty: EdgeDirty) {
        dirtyBox.put(dirty)
    }

    fun close() {
        boxStore.close()
    }

    private fun findRecord(entityType: String, _id: String): EdgeRecord? {
        val query = recordBox.query()
            .equal(EdgeRecord_.entityType, entityType, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .equal(EdgeRecord_._id, _id, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
        val result = query.findFirst()
        query.close()
        return result
    }

    /**
     * Checks if an entity matches all the given filters.
     */
    private fun matchesFilters(entity: Any, filters: List<EdgeFilter>): Boolean {
        for (filter in filters) {
            if (!matchesFilter(entity, filter)) {
                return false
            }
        }
        return true
    }

    /**
     * Checks if an entity matches a single filter using reflection.
     */
    private fun matchesFilter(entity: Any, filter: EdgeFilter): Boolean {
        val fieldValue = getFieldValue(entity, filter.field) ?: return false

        return when (filter.op) {
            Op.EQ -> fieldValue == filter.value
            Op.IN -> {
                val values = filter.value as? List<*> ?: return false
                values.contains(fieldValue)
            }
            Op.GT -> compareValues(fieldValue, filter.value) > 0
            Op.LT -> compareValues(fieldValue, filter.value) < 0
        }
    }

    /**
     * Gets a field value from an entity using reflection.
     */
    private fun getFieldValue(entity: Any, fieldName: String): Any? {
        return try {
            val field = entity.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(entity)
        } catch (e: NoSuchFieldException) {
            // Try getter method
            try {
                val getter = entity.javaClass.getMethod("get${fieldName.replaceFirstChar { it.uppercase() }}")
                getter.invoke(entity)
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compares two values for GT/LT operations.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compareValues(a: Any, b: Any): Int {
        return when {
            a is Number && b is Number -> a.toDouble().compareTo(b.toDouble())
            a is Comparable<*> && b is Comparable<*> -> {
                (a as Comparable<Any>).compareTo(b)
            }
            else -> 0
        }
    }
}
