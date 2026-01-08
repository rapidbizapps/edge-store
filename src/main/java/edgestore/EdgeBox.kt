package edgestore

import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.QueryBuilder

/**
 * Internal ObjectBox wrapper that resolves entities by business _id (never ObjectBox id).
 * Exposes minimal put, remove, query helpers with NO business logic or validation.
 */
internal class EdgeBox(private val boxStore: BoxStore) {

    // Cache for Property lookups per entity class and field name
    private val propertyCache = mutableMapOf<Pair<Class<*>, String>, Property<*>>()

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
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> remove(entityClass: Class<T>, _ids: List<String>) {
        if (_ids.isEmpty()) return

        val box = boxStore.boxFor(entityClass)
        val idProperty = getProperty(entityClass, "_id") as Property<T>
        
        val query = box.query()
        if (_ids.size == 1) {
            query.equal(idProperty, _ids[0], QueryBuilder.StringOrder.CASE_SENSITIVE)
        } else {
            query.`in`(idProperty, _ids.toTypedArray(), QueryBuilder.StringOrder.CASE_SENSITIVE)
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
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> query(entityClass: Class<T>, filters: List<EdgeFilter>): List<T> {
        val box = boxStore.boxFor(entityClass)
        var query = box.query()

        for (filter in filters) {
            val property = getProperty(entityClass, filter.field) as Property<T>
            
            query = when (filter.op) {
                Op.EQ -> when (filter.value) {
                    is String -> query.equal(property, filter.value, QueryBuilder.StringOrder.CASE_SENSITIVE)
                    is Int -> query.equal(property, filter.value.toLong())
                    is Long -> query.equal(property, filter.value)
                    is Boolean -> query.equal(property, filter.value)
                    else -> throw IllegalArgumentException("Unsupported EQ filter value type: ${filter.value::class}")
                }
                Op.IN -> when (filter.value) {
                    is List<*> -> {
                        val values = filter.value.filterNotNull()
                        if (values.isEmpty()) continue
                        when (values[0]) {
                            is String -> query.`in`(property, values.map { it as String }.toTypedArray(), QueryBuilder.StringOrder.CASE_SENSITIVE)
                            is Int -> query.`in`(property, values.map { (it as Int).toLong() }.toLongArray())
                            is Long -> query.`in`(property, values.map { it as Long }.toLongArray())
                            else -> throw IllegalArgumentException("Unsupported IN filter value type: ${values[0]!!::class}")
                        }
                    }
                    else -> throw IllegalArgumentException("IN operation requires List value")
                }
                Op.GT -> when (filter.value) {
                    is Int -> query.greater(property, filter.value.toLong())
                    is Long -> query.greater(property, filter.value)
                    else -> throw IllegalArgumentException("Unsupported GT filter value type: ${filter.value::class}")
                }
                Op.LT -> when (filter.value) {
                    is Int -> query.less(property, filter.value.toLong())
                    is Long -> query.less(property, filter.value)
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
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getById(entityClass: Class<T>, _id: String): T? {
        val box = boxStore.boxFor(entityClass)
        val idProperty = getProperty(entityClass, "_id") as Property<T>
        
        val query = box.query()
            .equal(idProperty, _id, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
        val result = query.findFirst()
        query.close()
        return result
    }

    fun close() {
        boxStore.close()
    }

    /**
     * Gets an ObjectBox Property object for a given entity class and field name.
     * Uses reflection to find the Property from the generated EntityName_ class.
     */
    @Suppress("UNCHECKED_CAST")
    private fun getProperty(entityClass: Class<*>, fieldName: String): Property<*> {
        val cacheKey = entityClass to fieldName
        propertyCache[cacheKey]?.let { return it }

        // ObjectBox generates a class named "EntityName_" with static Property fields
        val underscoreClassName = "${entityClass.name}_"
        val underscoreClass = try {
            Class.forName(underscoreClassName)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException(
                "Generated ObjectBox class $underscoreClassName not found. " +
                "Ensure ObjectBox annotation processing is configured for ${entityClass.simpleName}.",
                e
            )
        }

        // Find the Property field matching the fieldName
        val propertyField = try {
            underscoreClass.getField(fieldName)
        } catch (e: NoSuchFieldException) {
            throw IllegalArgumentException(
                "Property '$fieldName' not found in ${entityClass.simpleName}. " +
                "Ensure the field exists and is annotated properly.",
                e
            )
        }

        val property = propertyField.get(null) as Property<*>
        propertyCache[cacheKey] = property
        return property
    }
}
