package edgestore

import android.content.Context
import android.util.Log
import io.objectbox.BoxStore
import io.objectbox.android.Admin
import java.io.File


/**
 * Application-facing initializer that wires up EdgeStore instances for one or more
 * ObjectBox-backed local data stores.
 *
 * Two initialization modes:
 * 1. **External BoxStore** (recommended): App provides BoxStore with all entities.
 *    Use this for direct entity CRUD where entities extend BaseModel.
 *
 * 2. **Internal BoxStore**: Edge-store creates its own BoxStore.
 *    Use this for JSON serialization mode where user entities are stored in EdgeRecord.
 */
class EdgeStoreInitializer {

    private val appContext: Context?
    private val externalBoxStore: BoxStore?
    private val config: EdgeStoreConfig
    private val lock = Any()
    private val stores = mutableMapOf<String, EdgeStore>()
    private val boxStores = mutableMapOf<String, BoxStore>()

    /**
     * Initialize with an external BoxStore provided by the app.
     * The BoxStore should include all entities (app's + edge-store's).
     */
    constructor(boxStore: BoxStore, config: EdgeStoreConfig = EdgeStoreConfig()) {
        this.externalBoxStore = boxStore
        this.config = config
        this.appContext = null
        // Register the external BoxStore as the default store
        boxStores["default"] = boxStore
    }

    /**
     * Initialize with Android context - creates internal BoxStore.
     * Use for JSON serialization mode only.
     */
    constructor(context: Context, config: EdgeStoreConfig = EdgeStoreConfig()) {
        this.appContext = context.applicationContext
        this.config = config
        this.externalBoxStore = null
    }

    /**
     * Returns an EdgeStore for the given [storeName], creating it if necessary.
     *
     * If initialized with external BoxStore, uses that store directly.
     * Otherwise, creates an ObjectBox database at <app files dir>/objectbox/<storeName>.
     */
    fun getOrCreate(storeName: String): EdgeStore {
        synchronized(lock) {
            stores[storeName]?.let { return it }

            val boxStore = if (externalBoxStore != null && storeName == "default") {
                // Use the external BoxStore for the default store
                externalBoxStore
            } else if (appContext != null) {
                // Build internal BoxStore
                buildBoxStore(storeName)
            } else {
                throw IllegalStateException(
                    "Cannot create store '$storeName'. External BoxStore only supports 'default' store name."
                )
            }

            boxStores[storeName] = boxStore
            val edgeStore = EdgeStoreFactory.create(boxStore, config)
            stores[storeName] = edgeStore
            return edgeStore
        }
    }

    /**
     * Returns the underlying BoxStore for admin/debugging purposes.
     * Internal use only.
     */
    internal fun getBoxStore(storeName: String): BoxStore? {
        synchronized(lock) {
            return boxStores[storeName]
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

    private fun buildBoxStore(storeName: String): BoxStore {
        val context = appContext
            ?: throw IllegalStateException("Cannot build BoxStore without context")

        val dbDir = File(context.filesDir, "objectbox/$storeName")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        // Use the library's internal MyObjectBox - not the app's
        val boxStore1 = MyObjectBox.builder()
            .androidContext(context)
            .directory(dbDir)
            .build()
        val started = Admin(boxStore1).start(context)
        Log.i("ObjectBoxAdmin", "Started: $started")
        return boxStore1
    }
}
