package edgestore

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique
import java.util.UUID

/**
 * Base interface that all EdgeStore entities must implement.
 *
 * This interface ensures all entities have:
 * - id: Database-generated primary key
 * - _id: Business identifier (your app's unique ID)
 *
 * Example:
 * ```kotlin
 * @Entity
 * data class Task(
 *     @Id override var id: Long = 0,
 *     @Index override var _id: String = UUID.randomUUID().toString(),
 *     @Index var status: String = "pending",
 *     var name: String = ""
 * ) : BaseEntity
 * ```
 */
@BaseEntity
open class BaseModel {
    /**
     * ObjectBox primary key. Auto-assigned, do not set manually.
     */
    @Id
    var id: Long = 0

    /**
     * Business identifier used for lookups and references.
     * Indexed for fast queries.
     */
    @Index
    open var _id: String = ""

    /**
     * Timestamp when this entity was first created/persisted.
     */
    var createdAt: Long = 0

    /**
     * Timestamp when this entity was last updated.
     */
    var updatedAt: Long = 0
}