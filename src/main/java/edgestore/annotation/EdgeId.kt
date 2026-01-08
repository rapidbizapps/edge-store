package edgestore.annotation

/**
 * Marks a field as the primary key for an EdgeStore entity.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class EdgeId
