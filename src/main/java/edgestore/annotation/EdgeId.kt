package edgestore.annotation

import io.objectbox.annotation.Id

/**
 * Marks a field as the primary key for an EdgeStore entity.
 * This is an alias for ObjectBox @Id annotation.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Id
annotation class EdgeId
