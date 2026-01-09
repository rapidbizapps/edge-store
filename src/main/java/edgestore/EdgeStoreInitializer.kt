package edgestore

import android.content.Context
import edgestore.internal.EdgeStoreFactory

/**
 * Application-facing initializer that wires up EdgeStore instances for one or more
 * local data stores.
 */
class EdgeStoreInitializer(
    context: Context,
    private val config: EdgeStoreConfig = EdgeStoreConfig()
) {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val stores = mutableMapOf<String, EdgeStore>()

    /**
     * Returns an EdgeStore for the given [storeName] and [engine], creating it if necessary.
     */
    fun getOrCreate(storeName: String, engine: EdgeStoreEngine): EdgeStore {
        synchronized(lock) {
            stores[storeName]?.let { return it }

            val edgeStore = EdgeStoreFactory.create(appContext, storeName, engine, config)
            stores[storeName] = edgeStore
            return edgeStore
        }
    }

    /**
     * Returns a previously created EdgeStore by name, or null if it has not been created.
     */
    fun get(storeName: String): EdgeStore? = synchronized(lock) { stores[storeName] }

    /**
     * Closes a specific EdgeStore and its underlying BoxStore, removing it from the cache.
     */
    fun close(storeName: String) {
        synchronized(lock) {
            stores.remove(storeName)?.close()
        }
    }

    /**
     * Closes all cached EdgeStores and BoxStores.
     */
    fun closeAll() {
        synchronized(lock) {
            stores.values.forEach { it.close() }
            stores.clear()
        }
    }
}
