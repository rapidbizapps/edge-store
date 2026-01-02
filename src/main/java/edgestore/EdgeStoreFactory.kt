package edgestore

import io.objectbox.BoxStore

/**
 * Factory for creating EdgeStore instances.
 * Accepts a BoxStore from the consuming app and returns an EdgeStore.
 */
object EdgeStoreFactory {

    /**
     * Creates an EdgeStore instance using the provided BoxStore.
     * @param boxStore The BoxStore instance from the app.
     * @param config Configuration for EdgeStore (optional, defaults provided).
     * @return An EdgeStore instance.
     */
    fun create(boxStore: BoxStore, config: EdgeStoreConfig = EdgeStoreConfig()): EdgeStore {
        val edgeBox = EdgeBox(boxStore)
        return EdgeStoreImpl(edgeBox, config)
    }
}
