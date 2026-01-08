package edgestore.annotation

/**
 * Marks a class as an EdgeStore entity that will be persisted.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EdgeModel
