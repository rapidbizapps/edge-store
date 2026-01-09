package edgestore

import kotlin.reflect.KClass

/**
 * Generic base DAO that provides type-safe query operations for entities.
 *
 * Subclass this to create per-entity DAO singletons with custom query methods:
 * ```
 * object Tasks : EntityDao<Task>(Task::class, TaskEntity) {
 *     fun findByStatus(status: String): List<Task> =
 *         find(listOf(EdgeFilter("status", Op.EQ, status)))
 * }
 *
 * // Usage
 * val allTasks = Tasks.findAll()
 * val runningTasks = Tasks.findByStatus("running")
 * ```
 *
 * This pattern:
 * - Keeps entities "pure" (no query methods in companion objects)
 * - Provides type-safe queries without exposing ObjectBox/Room APIs
 * - Allows custom query methods per entity via subclassing
 * - Delegates all operations to Edge.session().core
 *
 * @param T The entity type this DAO operates on
 * @param entityClass The KClass of the entity type
 * @param entity The EdgeEntity descriptor for type-safe operations
 */
abstract class EntityDao<T : Any>(
    protected val entityClass: KClass<T>,
    protected val entity: EdgeEntity<T>
) {

    /**
     * Returns the EdgeCore for database operations.
     * Lazily accesses Edge.session() to allow late initialization.
     */
    protected val core: EdgeCore
        get() = Edge.session().core

    /**
     * Find all entities of this type.
     *
     * @return List of all entities
     */
    fun findAll(): List<T> = core.findAll(entity)

    /**
     * Find a single entity by its business identifier (_id).
     *
     * @param remoteId The business identifier to search for
     * @return The entity if found, null otherwise
     */
    fun findById(remoteId: String): T? = core.findById(entity, remoteId)

    /**
     * Find entities matching the given filters.
     *
     * @param filters List of EdgeFilter conditions to apply
     * @return List of matching entities
     */
    fun find(filters: List<EdgeFilter>): List<T> = core.find(entity, filters)

    /**
     * Find entities where the specified field value is in the given list.
     *
     * @param field The field name to filter on
     * @param values The list of values to match against
     * @return List of entities where field value is in the provided list
     */
    fun findIn(field: String, values: List<Any>): List<T> = core.findIn(entity, field, values)

    /**
     * Check if an entity with the given business identifier exists.
     *
     * @param remoteId The business identifier to check
     * @return true if entity exists, false otherwise
     */
    fun exists(remoteId: String): Boolean = findById(remoteId) != null

    /**
     * Count all entities of this type.
     *
     * @return The total count of entities
     */
    fun count(): Int = findAll().size

    /**
     * Access to the underlying EdgeStore for mutations.
     * Subclasses can use this for create/update/delete operations.
     */
    protected fun store(): EdgeStore = core.store()
}
