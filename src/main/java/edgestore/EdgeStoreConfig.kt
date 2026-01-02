package edgestore

import edgestore.util.JsonSerializer
import edgestore.util.Serializer

/**
 * Configuration for EdgeStore, including pluggable serializer.
 */
data class EdgeStoreConfig(
    val serializer: Serializer = JsonSerializer()
)
