package edgestore

/**
 * Entity descriptor for type-safe entity identification.
 */
interface EdgeEntity<T : Any> {
    val name: String
    val clazz: Class<T>
}

/**
 * Query operation types for filters.
 */
enum class Op {
    EQ, IN, GT, LT, NEQ, NULLORMISSING, NOTNULL
}

enum class OpC {
    AND,OR,JOIN
}

/**
 * Structured query filter for explicit, debuggable queries.
 */
data class EdgeFilter(
    val field: String,
    val op: Op,
    val value: Any,
    val joinOp: OpC? = null
)

/**
 * Mutation context for tracking operation source and metadata.
 */
data class EdgeContext(
    val source: String,   // "ui", "sync", "p2p"
    val actor: String? = null,
    val reason: String? = null
)

/**
 * The public API for EdgeStore, enforcing CRUD operations exclusively through this interface.
 * Application developers must never call ObjectBox APIs directly.
 */
interface EdgeStore {

    /**
     * Creates a new entity instance.
     * @param entity The entity descriptor.
     * @param data The entity object to store.
     * @param ctx The mutation context.
     * @return The business identifier (_id) of the created entity.
     */
    fun <T : Any> create(
        entity: EdgeEntity<T>,
        data: T,
        ctx: EdgeContext = EdgeContext("ui")
    ): String

    /**
     * Updates an existing entity.
     * @param entity The entity descriptor.
     * @param _id The business identifier of the entity to update.
     * @param data The updated entity object.
     * @param ctx The mutation context.
     */
    fun <T : Any> update(
        entity: EdgeEntity<T>,
        _id: String,
        data: T,
        ctx: EdgeContext = EdgeContext("ui")
    )

    /**
     * Deletes an entity.
     * @param entity The entity descriptor.
     * @param _id The business identifier of the entity to delete.
     * @param ctx The mutation context.
     */
    fun delete(
        entity: EdgeEntity<*>,
        _id: String,
        ctx: EdgeContext = EdgeContext("ui")
    )

    /**
     * Queries entities based on filters.
     * @param entity The entity descriptor.
     * @param filters A list of structured filters for querying.
     * @return A list of matching entities.
     */
    fun <T : Any> query(
        entity: EdgeEntity<T>,
        filters: List<EdgeFilter> = emptyList()
    ): List<T>

    /**
     * Closes the underlying resources for this EdgeStore instance.
     */
    fun close()
}
