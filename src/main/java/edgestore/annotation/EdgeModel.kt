package edgestore.annotation

import io.objectbox.annotation.Entity

/**
 * Marks a class as an EdgeStore entity that will be persisted.
 * This is an alias for ObjectBox @Entity annotation.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Entity
annotation class EdgeModel
