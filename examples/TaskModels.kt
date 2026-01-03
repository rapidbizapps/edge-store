package edgestore.example

import edgestore.EdgeEntity
import kotlinx.serialization.Serializable

/**
 * Represents a unit of work managed by the app.
 */
@Serializable
data class Task(
    val _id: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String
)

/**
 * Captures a single update in a task's lifecycle.
 */
@Serializable
data class TaskProgress(
    val _id: String,
    val taskId: String,
    val action: String,
    val timestamp: Long
)

/**
 * EdgeEntity descriptor for Task so it can be persisted with EdgeStore.
 */
object TaskEntity : EdgeEntity<Task> {
    override val name: String = "task"
    override val clazz: Class<Task> = Task::class.java
}

/**
 * EdgeEntity descriptor for TaskProgress so it can be persisted with EdgeStore.
 */
object TaskProgressEntity : EdgeEntity<TaskProgress> {
    override val name: String = "taskProgress"
    override val clazz: Class<TaskProgress> = TaskProgress::class.java
}
