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
    EQ, IN, GT, LT
}

/**
 * Structured query filter for explicit, debuggable queries.
 */
data class EdgeFilter(
    val field: String,
    val op: Op,
    val value: Any
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
     * @param payload The serialized payload of the entity.
     * @param ctx The mutation context.
     * @return The business identifier (_id) of the created entity.
     */
    fun create(
        entity: EdgeEntity,
        payload: ByteArray,
        ctx: EdgeContext = EdgeContext("ui")
    ): String

    /**
     * Updates an existing entity.
     * @param entity The entity descriptor.
     * @param _id The business identifier of the entity to update.
     * @param payload The serialized payload of the updated entity.
     * @param ctx The mutation context.
     */
    fun update(
        entity: EdgeEntity,
        _id: String,
        payload: ByteArray,
        ctx: EdgeContext = EdgeContext("ui")
    )

    /**
     * Deletes an entity.
     * @param entity The entity descriptor.
     * @param _id The business identifier of the entity to delete.
     * @param ctx The mutation context.
     */
    fun delete(
        entity: EdgeEntity,
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
        entity: EdgeEntity,
        filters: List<EdgeFilter>
    ): List<T>
}
