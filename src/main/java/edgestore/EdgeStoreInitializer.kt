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
 * The initializer owns ObjectBox bootstrapping internally - the application does NOT
 * need ObjectBox annotations or the ObjectBox plugin. All ObjectBox entities are
 * internal to the edge-store library.
 */
class EdgeStoreInitializer(
    context: Context,
) {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val stores = mutableMapOf<String, EdgeStore>()
    private val boxStores = mutableMapOf<String, BoxStore>()

    /**
     * Returns an EdgeStore for the given [storeName], creating it if necessary.
     * Creates an ObjectBox database directory at <app files dir>/objectbox/<storeName>
     * and builds a BoxStore using the library's internal MyObjectBox.
     */
    fun getOrCreate(storeName: String): EdgeStore {
        synchronized(lock) {
            stores[storeName]?.let { return it }

            val boxStore = buildBoxStore(storeName)
            boxStores[storeName] = boxStore
            val edgeStore = EdgeStoreFactory.create(boxStore)
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
        val dbDir = File(appContext.filesDir, "objectbox/$storeName")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        // Use the library's internal MyObjectBox - not the app's
        val boxStore1 = MyObjectBox.builder()
            .androidContext(appContext)
            .directory(dbDir)
            .build()
        val started = Admin(boxStore1).start(appContext)
        Log.i("ObjectBoxAdmin", "Started: " + started)
        return boxStore1
    }
}
