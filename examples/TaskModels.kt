package edgestore.example

import edgestore.EdgeEntity
import edgestore.annotation.EdgeId
import edgestore.annotation.EdgeModel
import kotlinx.serialization.Serializable

/**
 * Represents a unit of work managed by the app.
 */
@EdgeModel
@Serializable
data class Task(
    @EdgeId
    var id: Long = 0,
    val _id: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String
)

/**
 * Captures a single update in a task's lifecycle.
 */
@EdgeModel
@Serializable
data class TaskProgress(
    @EdgeId
    var id: Long = 0,
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
