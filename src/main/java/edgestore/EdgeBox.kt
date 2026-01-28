package edgestore

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.QueryBuilder

/**
 * ObjectBox wrapper that provides access to Box<T> for any Data class.
 * Single instance can work with multiple entity types.
 * Applies query filters directly in ObjectBox for efficient querying.
 *
 * @param boxStore The ObjectBox BoxStore instance
 */
internal class EdgeBox(private val boxStore: BoxStore) {

    /**
     * Gets the ObjectBox Box for the specified entity type.
     */
    fun <T : Any> boxFor(entityClass: Class<T>): Box<T> {
        return boxStore.boxFor(entityClass)
    }

    /**
     * Gets the ObjectBox Box for the specified entity type using reified type.
     */
    inline fun <reified T : Any> boxFor(): Box<T> {
        return boxStore.boxFor(T::class.java)
    }

    /**
     * Puts an entity into the store.
     */
    fun <T : Any> put(entity: T): Long {
        @Suppress("UNCHECKED_CAST")
        return boxFor(entity.javaClass as Class<T>).put(entity)
    }

    /**
     * Puts multiple entities into the store.
     */
    fun <T : Any> put(entities: List<T>, entityClass: Class<T>) {
        boxFor(entityClass).put(entities)
    }

    /**
     * Gets an entity by its ObjectBox ID.
     */
    fun <T : Any> get(id: Long, entityClass: Class<T>): T? {
        return boxFor(entityClass).get(id)
    }

    /**
     * Gets all entities of a type from the store.
     */
    fun <T : Any> getAll(entityClass: Class<T>): List<T> {
        return boxFor(entityClass).all
    }

    /**
     * Removes an entity by its ObjectBox ID.
     */
    fun <T : Any> remove(id: Long, entityClass: Class<T>): Boolean {
        return boxFor(entityClass).remove(id)
    }

    /**
     * Removes an entity.
     */
    fun <T : Any> remove(entity: T): Boolean {
        @Suppress("UNCHECKED_CAST")
        return boxFor(entity.javaClass as Class<T>).remove(entity)
    }

    /**
     * Removes all entities of a type from the store.
     */
    fun <T : Any> removeAll(entityClass: Class<T>) {
        boxFor(entityClass).removeAll()
    }

    /**
     * Returns the count of entities in the store.
     */
    fun <T : Any> count(entityClass: Class<T>): Long {
        return boxFor(entityClass).count()
    }

    /**
     * Creates a QueryBuilder for building custom queries.
     */
    fun <T : Any> query(entityClass: Class<T>): QueryBuilder<T> {
        return boxFor(entityClass).query()
    }

    /**
     * Executes a query with EdgeFilters applied directly in ObjectBox.
     */
    fun <T : Any> query(entityClass: Class<T>, filters: List<EdgeFilter>): List<T> {
        val box = boxFor(entityClass)

        if (filters.isEmpty()) {
            return box.all
        }

        val builder = box.query()
        applyFilters(builder, entityClass, filters)
        val query = builder.build()
        val results = query.find()
        query.close()
        return results
    }

    /**
     * Executes a custom query using a builder lambda.
     */
    fun <T : Any> query(
        entityClass: Class<T>,
        builderAction: (QueryBuilder<T>) -> QueryBuilder<T>
    ): List<T> {
        val builder = boxFor(entityClass).query()
        val configuredBuilder = builderAction(builder)
        val query = configuredBuilder.build()
        val results = query.find()
        query.close()
        return results
    }

    /**
     * Executes a query and returns only the first result.
     */
    fun <T : Any> queryFirst(
        entityClass: Class<T>,
        builderAction: (QueryBuilder<T>) -> QueryBuilder<T>
    ): T? {
        val builder = boxFor(entityClass).query()
        val configuredBuilder = builderAction(builder)
        val query = configuredBuilder.build()
        val result = query.findFirst()
        query.close()
        return result
    }

    /**
     * Executes a query with EdgeFilters and returns only the first result.
     */
    fun <T : Any> queryFirst(entityClass: Class<T>, filters: List<EdgeFilter>): T? {
        val box = boxFor(entityClass)
        val builder = box.query()
        applyFilters(builder, entityClass, filters)
        val query = builder.build()
        val result = query.findFirst()
        query.close()
        return result
    }

    fun close() {
        boxStore.close()
    }

    /**
     * Applies EdgeFilters to a QueryBuilder using ObjectBox Property API.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyFilters(
        builder: QueryBuilder<T>,
        entityClass: Class<T>,
        filters: List<EdgeFilter>
    ) {
        val propertiesClass = getPropertiesClass(entityClass) ?: return

        for (filter in filters) {
            val property = getProperty(propertiesClass, filter.field) ?: continue

            when (filter.op) {
                Op.EQ -> applyEqualFilter(builder, property, filter.value, filter.joinOp)
                Op.IN -> applyInFilter(builder, property, filter.value, filter.joinOp)
                Op.GT -> applyGreaterThanFilter(builder, property, filter.value, filter.joinOp)
                Op.LT -> applyLessThanFilter(builder, property, filter.value, filter.joinOp)
                Op.NOTNULL -> applyNotNullFilter(builder, property, filter.joinOp)
                Op.NULLORMISSING -> applyNullFilter(builder, property, filter.joinOp)
                Op.NEQ -> applyNotEqualFilter(builder, property, filter.value, filter.joinOp)
            }
        }
    }

    private fun <T : Any> applyNotEqualFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        value: Any,
        joinOp: OpC?
    ) {
        when (value) {
            is String -> builder.equal(
                property as Property<T>,
                value,
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )

            is Long -> builder.notEqual(property as Property<T>, value)
            is Int -> builder.notEqual(property as Property<T>, value.toLong())
            is Boolean -> builder.notEqual(property as Property<T>, value)

            is Double -> builder.notEqual(
                property as Property<T>,
                value.toString(),
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )

            is Float -> builder.notEqual(
                property as Property<T>,
                value.toString(),
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )
        }
        if (joinOp != null) {
            when (joinOp) {
                OpC.AND -> builder.and()
                OpC.OR -> builder.or()
                else -> {}
            }
        }
    }

    private fun <T : Any> applyNullFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        joinOp: OpC?
    ) {
        builder.isNull(property as Property<T>)
    }

    private fun <T : Any> applyNotNullFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        joinOp: OpC?
    ) {
        builder.notNull(property as Property<T>)
    }

    /**
     * Gets the ObjectBox generated properties class (e.g., Task_ for Task).
     */
    private fun getPropertiesClass(entityClass: Class<*>): Class<*>? {
        return try {
            Class.forName("${entityClass.name}_")
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    /**
     * Gets a Property from the properties class by field name.
     */
    private fun getProperty(propertiesClass: Class<*>, fieldName: String): Property<*>? {
        return try {
            val field = propertiesClass.getField(fieldName)
            field.get(null) as? Property<*>
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyEqualFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        value: Any,
        joinOp: OpC?
    ) {
        when (value) {
            is String -> builder.equal(
                property as Property<T>,
                value,
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )

            is Long -> builder.equal(property as Property<T>, value)
            is Int -> builder.equal(property as Property<T>, value.toLong())
            is Boolean -> builder.equal(property as Property<T>, value)
            is Double -> builder.equal(property as Property<T>, value, 0.0001)
            is Float -> builder.equal(property as Property<T>, value.toDouble(), 0.0001)
        }
        if (joinOp != null) {
            when (joinOp) {
                OpC.AND -> builder.and()
                OpC.OR -> builder.or()
                else -> {}
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyInFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        value: Any,
        joinOp: OpC?
    ) {
        val values = value as? List<*> ?: return
        when {
            values.all { it is String } -> {
                builder.`in`(
                    property as Property<T>,
                    values.filterIsInstance<String>().toTypedArray(),
                    QueryBuilder.StringOrder.CASE_SENSITIVE
                )
            }

            values.all { it is Long } -> {
                builder.`in`(property as Property<T>, values.filterIsInstance<Long>().toLongArray())
            }

            values.all { it is Int } -> {
                builder.`in`(
                    property as Property<T>,
                    values.filterIsInstance<Int>().map { it.toLong() }.toLongArray()
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyGreaterThanFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        value: Any,
        joinOp: OpC?
    ) {
        when (value) {
            is Long -> builder.greater(property as Property<T>, value)
            is Int -> builder.greater(property as Property<T>, value.toLong())
            is Double -> builder.greater(property as Property<T>, value)
            is Float -> builder.greater(property as Property<T>, value.toDouble())
            is String -> builder.greater(
                property as Property<T>,
                value,
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyLessThanFilter(
        builder: QueryBuilder<T>,
        property: Property<*>,
        value: Any,
        joinOp: OpC?
    ) {
        when (value) {
            is Long -> builder.less(property as Property<T>, value)
            is Int -> builder.less(property as Property<T>, value.toLong())
            is Double -> builder.less(property as Property<T>, value)
            is Float -> builder.less(property as Property<T>, value.toDouble())
            is String -> builder.less(
                property as Property<T>,
                value,
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )
        }
    }
}
