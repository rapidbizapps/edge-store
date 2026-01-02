package edgestore

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * ObjectBox entity for tracking mutations (create, update, delete) on entities.
 * This is the ONLY mutation tracking mechanism in EdgeStore.
 */
@Entity
class EdgeDirty {
    @Id
    var id: Long = 0

    @Index
    lateinit var entityType: String

    @Index
    lateinit var _id: String

    @Index
    lateinit var operation: String   // CREATE | UPDATE | DELETE

    @Index
    lateinit var source: String      // ui | sync | p2p

    var timestamp: Long = 0

    var actor: String? = null
    var reason: String? = null
}
