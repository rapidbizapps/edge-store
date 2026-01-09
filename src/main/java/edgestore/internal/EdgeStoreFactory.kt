package edgestore.internal

import android.content.Context
import edgestore.EdgeStore
import edgestore.EdgeStoreConfig
import edgestore.EdgeStoreEngine
import edgestore.objectbox.ObjectBoxEdgeStore

/**
 * Factory for creating EdgeStore instances for supported engines.
 * This remains internal to the SDK so application code cannot depend on engine-specific types.
 */
internal object EdgeStoreFactory {

    /**
     * Creates an EdgeStore instance using the provided engine.
     */
    fun create(
        context: Context,
        storeName: String,
        engine: EdgeStoreEngine,
        config: EdgeStoreConfig = EdgeStoreConfig()
    ): EdgeStore {
        return when (engine) {
            EdgeStoreEngine.OBJECTBOX -> ObjectBoxEdgeStore.create(context, storeName, config)
        }
    }
}
