package edgestore

/**
 * The public API for EdgeStore, enforcing CRUD operations exclusively through this interface.
 * Application developers must never call ObjectBox APIs directly.
 */
interface EdgeStore {

    /**
     * Creates a new entity instance.
     * @param entityClass The class of the entity to create.
     * @param payload The serialized payload of the entity.
     * @return The business identifier (_id) of the created entity.
     */
    fun create(
        entityClass: Class<*>,
        payload: ByteArray
    ): String

    /**
     * Updates an existing entity.
     * @param entityClass The class of the entity to update.
     * @param _id The business identifier of the entity to update.
     * @param payload The serialized payload of the updated entity.
     */
    fun update(
        entityClass: Class<*>,
        _id: String,
        payload: ByteArray
    )

    /**
     * Deletes an entity.
     * @param entityClass The class of the entity to delete.
     * @param _id The business identifier of the entity to delete.
     */
    fun delete(
        entityClass: Class<*>,
        _id: String
    )

    /**
     * Queries entities based on filters.
     * @param entityClass The class of the entities to query.
     * @param filters A map of field names to values for filtering.
     * @return A list of matching entities.
     */
    fun <T : Any> query(
        entityClass: Class<T>,
        filters: Map<String, Any>
    ): List<T>
}
