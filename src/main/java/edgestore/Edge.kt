package edgestore

import android.content.Context

/**
 * Central access point for EdgeStore operations.
 *
 * Provides a session-like interface that EntityDao delegates to for all
 * database operations. This keeps ObjectBox/Room APIs completely hidden
 * from application code.
 *
 * Usage:
 * ```
 * // During app initialization
 * Edge.init(context, EdgeStoreConfig())
 *
 * // Later, DAOs automatically use Edge.session()
 * val tasks = Tasks.findAll()
 * ```
 */
object Edge {

    @Volatile
    private var initializer: EdgeStoreInitializer? = null

    @Volatile
    private var defaultStoreName: String = "default"

    private val lock = Any()

    /**
     * Initialize Edge with an Android context and configuration.
     * Must be called before any DAO operations.
     *
     * @param context Android application context
     * @param config EdgeStore configuration (serializer, etc.)
     * @param defaultStoreName The default store name to use when none is specified
     */
    fun init(
        context: Context,
        config: EdgeStoreConfig = EdgeStoreConfig(),
        defaultStoreName: String = "default"
    ) {
        synchronized(lock) {
            if (initializer != null) {
                throw IllegalStateException("Edge is already initialized")
            }
            this.defaultStoreName = defaultStoreName
            this.initializer = EdgeStoreInitializer(context, config)
        }
    }

    /**
     * Returns the current EdgeSession for database operations.
     * EntityDao delegates all operations to this session.
     *
     * @throws IllegalStateException if Edge.init() has not been called
     */
    fun session(): EdgeSession {
        val init = initializer
            ?: throw IllegalStateException("Edge not initialized. Call Edge.init(context) first.")
        return EdgeSession(init, defaultStoreName)
    }

    /**
     * Returns a session for a specific store name.
     * Useful when working with multiple databases.
     */
    fun session(storeName: String): EdgeSession {
        val init = initializer
            ?: throw IllegalStateException("Edge not initialized. Call Edge.init(context) first.")
        return EdgeSession(init, storeName)
    }

    /**
     * Closes all stores and resets Edge state.
     * Call during application shutdown or testing.
     */
    fun shutdown() {
        synchronized(lock) {
            initializer?.closeAll()
            initializer = null
        }
    }

    /**
     * Check if Edge has been initialized.
     */
    fun isInitialized(): Boolean = initializer != null

    /**
     * Returns the underlying BoxStore for admin/debugging purposes.
     * Internal use only - used by EdgeAdmin.
     */
    internal fun getBoxStore(storeName: String = defaultStoreName): Any? {
        return initializer?.getBoxStore(storeName)
    }
}

/**
 * Session wrapper that provides type-safe query operations.
 * This is what EntityDao delegates to - it never exposes ObjectBox types.
 */
class EdgeSession internal constructor(
    private val initializer: EdgeStoreInitializer,
    private val storeName: String
) {
    /**
     * The core operations interface that EntityDao uses.
     */
    val core: EdgeCore by lazy { EdgeCore(initializer.getOrCreate(storeName)) }
}

/**
 * Core operations interface for EntityDao.
 * Provides type-safe query methods without exposing ObjectBox.
 */
class EdgeCore internal constructor(private val store: EdgeStore) {

    /**
     * Find all entities of the given type.
     */
    fun <T : Any> findAll(entity: EdgeEntity<T>): List<T> {
        return store.query(entity, emptyList())
    }

    /**
     * Find a single entity by its business identifier (_id).
     */
    fun <T : Any> findById(entity: EdgeEntity<T>, remoteId: String): T? {
        val results: List<T> = store.query(entity, listOf(EdgeFilter("_id", Op.EQ, remoteId)))
        return results.firstOrNull()
    }

    /**
     * Find entities matching the given filters.
     */
    fun <T : Any> find(entity: EdgeEntity<T>, filters: List<EdgeFilter>): List<T> {
        return store.query(entity, filters)
    }

    /**
     * Find entities where a field value is in the given list.
     */
    fun <T : Any> findIn(
        entity: EdgeEntity<T>,
        field: String,
        values: List<Any>
    ): List<T> {
        if (values.isEmpty()) return emptyList()
        return store.query(entity, listOf(EdgeFilter(field, Op.IN, values)))
    }

    /**
     * Access to the underlying EdgeStore for advanced operations (create, update, delete).
     * DAOs can use this for mutations while keeping queries type-safe.
     */
    fun store(): EdgeStore = store
}
