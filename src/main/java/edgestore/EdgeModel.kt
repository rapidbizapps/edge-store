package edgestore

/**
 * Helper function to create an EdgeEntity descriptor for a model class.
 *
 * Example usage:
 * ```
 * data class User(
 *     override val _id: String,
 *     val name: String
 * ) : BaseModel
 *
 * // Create entity descriptor using helper function
 * val UserEntity = edgeEntity<User>("user")
 *
 * // Or manually:
 * object UserEntity : EdgeEntity<User> {
 *     override val name = "user"
 *     override val clazz = User::class.java
 * }
 *
 * // Use with EntityDao
 * object Users : EntityDao<User>(User::class, UserEntity) {
 *     fun findByName(name: String) = find(listOf(EdgeFilter("name", Op.EQ, name)))
 * }
 * ```
 */
inline fun <reified T : Any> edgeEntity(name: String): EdgeEntity<T> = object : EdgeEntity<T> {
    override val name: String = name
    override val clazz: Class<T> = T::class.java
}
