package edgestore

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

/**
 * Internal ObjectBox entity for storing all user data as serialized JSON.
 * This keeps ObjectBox completely hidden from client applications.
 *
 * All user entities (Task, User, etc.) are stored as EdgeRecord entries
 * with their data serialized in the [payload] field.
 */
@Entity
internal class EdgeRecord {
    @Id
    var id: Long = 0

    /**
     * Business identifier - the user's primary key for the entity.
     * Unique within an entity type.
     */
    @Index
    lateinit var _id: String

    /**
     * Entity type name (e.g., "task", "user").
     * Used to partition records by entity type.
     */
    @Index
    lateinit var entityType: String

    /**
     * Serialized JSON payload of the entity.
     */
    lateinit var payload: String

    /**
     * Timestamp when this record was created.
     */
    var createdAt: Long = 0

    /**
     * Timestamp when this record was last updated.
     */
    var updatedAt: Long = 0
}
