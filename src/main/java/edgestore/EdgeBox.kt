package edgestore

import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

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
     * Removes an entity by its business _id.
     */
    fun remove(entityClass: Class<*>, _id: String) {
        val box = boxStore.boxFor(entityClass)
        val query = box.query()
            .equal(getIdProperty(entityClass), _id, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
        val entities = query.find()
        if (entities.isNotEmpty()) {
            box.remove(entities)
        }
        query.close()
    }

    /**
     * Queries entities based on filters. Filters are field name to value mappings.
     */
    fun <T : Any> query(entityClass: Class<T>, filters: Map<String, Any>): List<T> {
        val box = boxStore.boxFor(entityClass)
        var query = box.query()

        for ((fieldName, value) in filters) {
            val property = getProperty(entityClass, fieldName)
            when (value) {
                is String -> query = query.equal(property, value, QueryBuilder.StringOrder.CASE_SENSITIVE)
                is Int -> query = query.equal(property, value.toLong())
                is Long -> query = query.equal(property, value)
                is Boolean -> query = query.equal(property, value)
                else -> throw IllegalArgumentException("Unsupported filter value type: ${value::class}")
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
            .equal(getIdProperty(entityClass), _id, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
        val result = query.findFirst()
        query.close()
        return result
    }

    private fun getIdProperty(entityClass: Class<*>): String {
        val kClass = entityClass.kotlin
        val idProperty = kClass.memberProperties.find { it.name == "_id" }
            ?: throw IllegalArgumentException("Entity ${entityClass.simpleName} must have a '_id' property")
        return idProperty.name
    }

    private fun getProperty(entityClass: Class<*>, propertyName: String): String {
        val kClass = entityClass.kotlin
        val property = kClass.memberProperties.find { it.name == propertyName }
            ?: throw IllegalArgumentException("Property $propertyName not found in ${entityClass.simpleName}")
        return property.name
    }
}
