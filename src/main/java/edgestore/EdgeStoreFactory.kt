package edgestore

import io.objectbox.BoxStore

/**
 * Factory for creating EdgeStore instances.
 * This remains internal to the SDK so application code cannot depend on ObjectBox types.
 */
internal object EdgeStoreFactory {

    /**
     * Creates an EdgeStore instance using the provided BoxStore.
     * @param boxStore The BoxStore instance managed inside the SDK.
     * @param config Configuration for EdgeStore (optional, defaults provided).
     * @return An EdgeStore instance.
     */
    fun create(boxStore: BoxStore, config: EdgeStoreConfig = EdgeStoreConfig()): EdgeStore {
        val edgeBox = EdgeBox(boxStore)
        return EdgeStoreImpl(edgeBox, config)
    }
}
