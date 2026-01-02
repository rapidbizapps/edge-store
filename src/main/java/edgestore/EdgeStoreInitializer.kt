package edgestore

import android.content.Context
import io.objectbox.BoxStore
import java.io.File

/**
 * Application-facing initializer that wires up EdgeStore instances for one or more
 * ObjectBox-backed local data stores.
 *
 * The initializer owns ObjectBox bootstrapping so the application only depends on
 * EdgeStore, EdgeStoreInitializer, and EdgeStoreConfig.
 */
class EdgeStoreInitializer(
    context: Context,
    private val config: EdgeStoreConfig = EdgeStoreConfig()
) {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val stores = mutableMapOf<String, EdgeStore>()

    /**
     * Returns an EdgeStore for the given [storeName], creating it if necessary.
     * Creates an ObjectBox database directory at <app files dir>/objectbox/<storeName>
     * and builds a BoxStore using the generated MyObjectBox builder inside the app.
     */
    fun getOrCreate(storeName: String): EdgeStore {
        synchronized(lock) {
            stores[storeName]?.let { return it }

            val boxStore = buildBoxStore(storeName)
            val edgeStore = EdgeStoreFactory.create(boxStore, config)
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
        val edgeStore = synchronized(lock) { stores.remove(storeName) }
        edgeStore?.close()
    }

    /**
     * Closes all cached EdgeStores and BoxStores.
     */
    fun closeAll() {
        val edgeStores = synchronized(lock) {
            val current = stores.values.toList()
            stores.clear()
            current
        }
        edgeStores.forEach { it.close() }
    }

    private fun buildBoxStore(storeName: String): BoxStore {
        val dbDir = File(appContext.filesDir, "objectbox/$storeName")
        val databaseExists = BoxStore.exists(dbDir.absolutePath)
        if (!databaseExists && !dbDir.exists()) {
            dbDir.mkdirs()
        }

        val myObjectBoxClass = try {
            Class.forName("${appContext.packageName}.MyObjectBox")
        } catch (ex: ClassNotFoundException) {
            throw IllegalStateException(
                "MyObjectBox not found. Ensure ObjectBox annotation processing is configured.",
                ex
            )
        }

        val builderMethod = myObjectBoxClass.getMethod("builder")
        val builder = builderMethod.invoke(null)
        val builderClass = builder.javaClass

        // Prefer the Android-aware builder configuration when available.
        val androidContextMethod = builderClass.methods.firstOrNull {
            it.name == "androidContext" && it.parameterTypes.size == 1 &&
                Context::class.java.isAssignableFrom(it.parameterTypes[0])
        }
        androidContextMethod?.invoke(builder, appContext)

        builderClass.getMethod("directory", File::class.java).invoke(builder, dbDir)

        val buildMethod = builderClass.getMethod("build")
        return buildMethod.invoke(builder) as BoxStore
    }
}
