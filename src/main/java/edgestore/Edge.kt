package edgestore

import android.content.Context
import io.objectbox.BoxStore

/**
 * Central access point for EdgeStore operations.
 *
 * Provides a session-like interface that EntityDao delegates to for all
 * database operations. This keeps ObjectBox/Room APIs completely hidden
 * from application code.
 *
 * Usage with app-provided BoxStore (recommended for direct entity CRUD):
 * ```
 * // In Application.onCreate()
 * val boxStore = MyObjectBox.builder()
 *     .androidContext(this)
 *     .build()
 * Edge.init(boxStore)
 *
 * // Later, DAOs automatically use Edge.session()
 * val tasks = Tasks.findAll()
 * ```
 *
 * Usage with context (for JSON serialization mode):
 * ```
 * Edge.init(context, EdgeStoreConfig())
 * ```
 */
object Edge {

    @Volatile
    private var initializer: EdgeStoreInitializer? = null

    @Volatile
    private var defaultStoreName: String = "default"

    private val lock = Any()

    /**
     * Initialize Edge with an app-provided BoxStore.
     *
     * Use this when your app creates entities that extend BaseModel.
     * The BoxStore should be built with your app's MyObjectBox which includes
     * both your entities (Task, User, etc.) and edge-store's entities (EdgeRecord, EdgeDirty).
     *
     * @param boxStore The app's BoxStore with all entities registered
     * @param config EdgeStore configuration (serializer, etc.)
     */
    fun init(
        boxStore: BoxStore,
        config: EdgeStoreConfig = EdgeStoreConfig()
    ) {
        synchronized(lock) {
            if (initializer != null) {
                throw IllegalStateException("Edge is already initialized")
            }
            this.defaultStoreName = "default"
            this.initializer = EdgeStoreInitializer(boxStore, config)
        }
    }

    /**
     * Initialize Edge with an Android context and configuration.
     * This creates an internal BoxStore - use only for JSON serialization mode.
     *
     * For direct entity CRUD, use init(boxStore, config) instead.
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
 *
 * Supports two modes:
 * 1. **Direct mode**: Use findAllDirect(), putDirect() etc. for entities extending BaseModel
 * 2. **JSON mode**: Use findAll(), find() etc. for JSON serialization into EdgeRecord
 */
class EdgeCore internal constructor(private val store: EdgeStore) {

    // =========================================================================
    // Direct Entity Mode (Recommended for entities extending BaseModel)
    // =========================================================================

    /**
     * Find all entities of the given type (direct mode).
     */
    fun <T : Any> findAllDirect(entity: EdgeEntity<T>): List<T> {
        return store.findAllDirect(entity.clazz)
    }

    /**
     * Find entity by business _id (direct mode).
     */
    fun <T : Any> findByIdDirect(entity: EdgeEntity<T>, _id: String): T? {
        return store.findByBusinessIdDirect(entity.clazz, _id)
    }

    /**
     * Save an entity (direct mode).
     */
    fun <T : Any> putDirect(entity: T): Long {
        return store.putDirect(entity)
    }

    /**
     * Save multiple entities (direct mode).
     */
    fun <T : Any> putAllDirect(entities: List<T>) {
        store.putAllDirect(entities)
    }

    /**
     * Remove entity by ObjectBox ID (direct mode).
     */
    fun <T : Any> removeDirect(entityDescriptor: EdgeEntity<T>, id: Long) {
        store.removeDirect(entityDescriptor.clazz, id)
    }

    // =========================================================================
    // JSON Serialization Mode (Legacy - for entities stored in EdgeRecord)
    // =========================================================================

    /**
     * Find all entities of the given type (JSON mode).
     */
    fun <T : Any> findAll(entity: EdgeEntity<T>): List<T> {
        return store.findAllDirect(entity.clazz)
    }

    /**
     * Find a single entity by its business identifier (_id) (JSON mode).
     */
    fun <T : Any> findById(entity: EdgeEntity<T>, remoteId: String): T? {
        return store.findByBusinessIdDirect(entity.clazz, remoteId)
    }

    /**
     * Find entities matching the given filters (JSON mode).
     * Note: For direct mode, consider using ObjectBox queries directly.
     */
    fun <T : Any> find(entity: EdgeEntity<T>, filters: List<EdgeFilter>): List<T> {
        // For now, use direct mode and filter in memory
        val all = store.findAllDirect(entity.clazz)
        if (filters.isEmpty()) return all
        return all.filter { matchesFilters(it, filters) }
    }

    /**
     * Find entities where a field value is in the given list (JSON mode).
     */
    fun <T : Any> findIn(
        entity: EdgeEntity<T>,
        field: String,
        values: List<Any>
    ): List<T> {
        if (values.isEmpty()) return emptyList()
        return find(entity, listOf(EdgeFilter(field, Op.IN, values)))
    }

    /**
     * Access to the underlying EdgeStore for advanced operations.
     */
    fun store(): EdgeStore = store

    // =========================================================================
    // Filter matching (for in-memory filtering)
    // =========================================================================

    private fun matchesFilters(entity: Any, filters: List<EdgeFilter>): Boolean {
        return filters.all { matchesFilter(entity, it) }
    }

    private fun matchesFilter(entity: Any, filter: EdgeFilter): Boolean {
        val fieldValue = getFieldValue(entity, filter.field) ?: return false
        return when (filter.op) {
            Op.EQ -> fieldValue == filter.value
            Op.IN -> (filter.value as? List<*>)?.contains(fieldValue) ?: false
            Op.GT -> compareValues(fieldValue, filter.value) > 0
            Op.LT -> compareValues(fieldValue, filter.value) < 0
        }
    }

    private fun getFieldValue(entity: Any, fieldName: String): Any? {
        return try {
            val field = entity.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(entity)
        } catch (e: NoSuchFieldException) {
            // Try superclass
            try {
                val field = entity.javaClass.superclass?.getDeclaredField(fieldName)
                field?.isAccessible = true
                field?.get(entity)
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compareValues(a: Any, b: Any): Int {
        return when {
            a is Number && b is Number -> a.toDouble().compareTo(b.toDouble())
            a is Comparable<*> -> (a as Comparable<Any>).compareTo(b)
            else -> 0
        }
    }
}
