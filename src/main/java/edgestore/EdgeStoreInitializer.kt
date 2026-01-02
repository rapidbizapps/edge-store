package edgestore

import android.content.Context
import io.objectbox.BoxStore
import java.io.File

/**
 * Application-facing initializer that wires up EdgeStore instances for one or more
 * ObjectBox-backed local data stores.
 *
 * Responsibilities:
 * - Ensures ObjectBox database directory exists (creates it if missing).
 * - Accepts a caller-provided BoxStore builder to keep ObjectBox details out of app code.
 * - Produces and caches distinct EdgeStore instances for multiple named stores.
 */
class EdgeStoreInitializer(
    private val context: Context,
    private val config: EdgeStoreConfig = EdgeStoreConfig()
) {

    private val edgeStores = mutableMapOf<String, EdgeStore>()
    private val boxStores = mutableMapOf<String, BoxStore>()

    /**
     * Returns an EdgeStore for the given [storeName], creating it if necessary.
     * The [boxStoreBuilder] receives the database directory and must return a configured BoxStore
     * (e.g., `MyObjectBox.builder().directory(dbDir).build()`).
     *
     * If an ObjectBox database already exists at the directory, it will be reused; otherwise the
     * directory is created before building the store.
     */
    fun getOrCreate(
        storeName: String,
        boxStoreBuilder: (databaseDir: File) -> BoxStore
    ): EdgeStore {
        edgeStores[storeName]?.let { return it }

        val dbDir = File(context.filesDir, "objectbox/$storeName")
        val databaseExists = BoxStore.exists(dbDir.absolutePath)
        if (!databaseExists && !dbDir.exists()) {
            dbDir.mkdirs()
        }

        val boxStore = boxStores.getOrPut(storeName) {
            boxStoreBuilder(dbDir)
        }

        val edgeStore = EdgeStoreFactory.create(boxStore, config)
        edgeStores[storeName] = edgeStore
        return edgeStore
    }

    /**
     * Returns a previously created EdgeStore by name, or null if it has not been created.
     */
    fun get(storeName: String): EdgeStore? = edgeStores[storeName]
}
