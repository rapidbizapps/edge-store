package edgestore

import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder

/**
 * Internal ObjectBox wrapper that resolves entities by business _id (never ObjectBox id).
 * Exposes minimal put, remove, query helpers with NO business logic or validation.
 */
internal class EdgeBox(private val boxStore: BoxStore) {

    /**
     * Puts an entity into the store. If an entity with the same _id exists, it is replaced.
     */
    fun put(entity: Any) {
        val box = boxStore.boxFor(entity.javaClass)
        box.put(entity)
    }

    /**
     * Removes entities by their business _id.
     */
    fun remove(entityClass: Class<*>, _ids: List<String>) {
        if (_ids.isEmpty()) return

        val box = boxStore.boxFor(entityClass)
        val query = box.query()
        if (_ids.size == 1) {
            query.equal("_id", _ids[0], QueryBuilder.StringOrder.CASE_SENSITIVE)
        } else {
            query.`in`("_id", _ids.toTypedArray(), QueryBuilder.StringOrder.CASE_SENSITIVE)
        }

        val entities = query.build().find()
        if (entities.isNotEmpty()) {
            box.remove(entities)
        }
        query.close()
    }

    /**
     * Queries entities based on structured filters.
     */
    fun <T : Any> query(entityClass: Class<T>, filters: List<EdgeFilter>): List<T> {
        val box = boxStore.boxFor(entityClass)
        var query = box.query()

        for (filter in filters) {
            query = when (filter.op) {
                Op.EQ -> when (filter.value) {
                    is String -> query.equal(filter.field, filter.value, QueryBuilder.StringOrder.CASE_SENSITIVE)
                    is Int -> query.equal(filter.field, filter.value.toLong())
                    is Long -> query.equal(filter.field, filter.value)
                    is Boolean -> query.equal(filter.field, filter.value)
                    else -> throw IllegalArgumentException("Unsupported EQ filter value type: ${filter.value::class}")
                }
                Op.IN -> when (filter.value) {
                    is List<*> -> {
                        val values = filter.value.filterNotNull()
                        if (values.isEmpty()) continue
                        when (values[0]) {
                            is String -> query.`in`(filter.field, values.map { it as String }.toTypedArray(), QueryBuilder.StringOrder.CASE_SENSITIVE)
                            is Int -> query.`in`(filter.field, values.map { (it as Int).toLong() }.toTypedArray())
                            is Long -> query.`in`(filter.field, values.map { it as Long }.toTypedArray())
                            else -> throw IllegalArgumentException("Unsupported IN filter value type: ${values[0]!!::class}")
                        }
                    }
                    else -> throw IllegalArgumentException("IN operation requires List value")
                }
                Op.GT -> when (filter.value) {
                    is Int -> query.greater(filter.field, filter.value.toLong())
                    is Long -> query.greater(filter.field, filter.value)
                    else -> throw IllegalArgumentException("Unsupported GT filter value type: ${filter.value::class}")
                }
                Op.LT -> when (filter.value) {
                    is Int -> query.less(filter.field, filter.value.toLong())
                    is Long -> query.less(filter.field, filter.value)
                    else -> throw IllegalArgumentException("Unsupported LT filter value type: ${filter.value::class}")
                }
            }
        }

        val result = query.build().find()
        query.close()
        return result
    }

    /**
     * Retrieves an entity by its business _id.
     */
    fun <T : Any> getById(entityClass: Class<T>, _id: String): T? {
        val box = boxStore.boxFor(entityClass)
        val query = box.query()
            .equal("_id", _id, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
        val result = query.findFirst()
        query.close()
        return result
    }
}
