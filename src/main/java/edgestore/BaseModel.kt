package edgestore

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * Base class for all ObjectBox entities.
 *
 * Extend this class in your main project to get common fields with proper
 * ObjectBox annotations. Your entities only need `@Entity` annotation.
 *
 * **Important**: edge-store must be included as a source module (not binary JAR)
 * for ObjectBox annotation processor to see this @BaseEntity class.
 *
 * Example usage:
 * ```
 * import edgestore.Entity    // From edge-store, not io.objectbox!
 * import edgestore.BaseModel
 *
 * @Entity
 * class User() : BaseModel() {
 *     lateinit var name: String
 *     lateinit var email: String
 *
 *     constructor(_id: String, name: String, email: String) : this() {
 *         this._id = _id
 *         this.name = name
 *         this.email = email
 *     }
 * }
 * ```
 *
 * Provides these fields:
 * - `id: Long` - ObjectBox primary key (auto-assigned)
 * - `_id: String` - Business identifier (indexed for fast lookups)
 * - `createdAt: Long` - Creation timestamp
 * - `updatedAt: Long` - Last modification timestamp
 *
 * @property id ObjectBox primary key, auto-assigned. Do not set manually.
 * @property _id Business identifier for your application. Set this in constructor.
 * @property createdAt Timestamp (epoch millis) when entity was first persisted.
 * @property updatedAt Timestamp (epoch millis) when entity was last updated.
 */
@BaseEntity
open class BaseModel {
    /**
     * ObjectBox primary key. Auto-assigned, do not set manually.
     *
     * Note: This is 'open' so subclasses in other modules can redeclare it with @Id
     * annotation for the ObjectBox annotation processor to see it.
     */
    @Id
    open var id: Long = 0

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
