package edgestore

/**
 * Re-exported ObjectBox annotations.
 *
 * Import these from `edgestore` package instead of `io.objectbox.annotation`
 * to keep your main project free of direct ObjectBox dependencies.
 *
 * Example:
 * ```
 * import edgestore.Entity
 * import edgestore.BaseModel
 *
 * @Entity
 * class User() : BaseModel() {
 *     lateinit var name: String
 * }
 * ```
 */

// Core entity annotation
typealias Entity = io.objectbox.annotation.Entity

// Property annotations
typealias Id = io.objectbox.annotation.Id
typealias Index = io.objectbox.annotation.Index
typealias Unique = io.objectbox.annotation.Unique
typealias Transient = io.objectbox.annotation.Transient

// Relationship annotations
typealias Backlink = io.objectbox.annotation.Backlink

// Convert annotation for custom type converters
typealias Convert = io.objectbox.annotation.Convert

// UID annotation for schema migrations
typealias Uid = io.objectbox.annotation.Uid
