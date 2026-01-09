package edgestore.example

import edgestore.EdgeEntity
import edgestore.EdgeFilter
import edgestore.EntityDao
import edgestore.Op
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

// ============================================================================
// DAO Objects - Type-safe query interfaces for application code
// ============================================================================

/**
 * DAO for Task entities.
 *
 * Usage:
 * ```
 * val allTasks = Tasks.findAll()
 * val task = Tasks.findById("task-001")
 * val runningTasks = Tasks.findByStatus("running")
 * ```
 */
object Tasks : EntityDao<Task>(Task::class, TaskEntity) {

    /**
     * Find all tasks with the given status.
     */
    fun findByStatus(status: String): List<Task> =
        find(listOf(EdgeFilter("status", Op.EQ, status)))

    /**
     * Find all tasks that are currently running (status = "running").
     */
    fun findRunning(): List<Task> = findByStatus("running")

    /**
     * Find all tasks that have completed (status = "completed").
     */
    fun findCompleted(): List<Task> = findByStatus("completed")
}

/**
 * DAO for TaskProgress entities.
 *
 * Usage:
 * ```
 * val allProgress = TaskProgresses.findAll()
 * val progress = TaskProgresses.findByTaskId("task-001")
 * ```
 */
object TaskProgresses : EntityDao<TaskProgress>(TaskProgress::class, TaskProgressEntity) {

    /**
     * Find all progress entries for a specific task.
     *
     * @param taskId The business identifier of the task
     * @return List of progress entries for the task, ordered by timestamp
     */
    fun findByTaskId(taskId: String): List<TaskProgress> =
        find(listOf(EdgeFilter("taskId", Op.EQ, taskId)))

    /**
     * Find all progress entries for multiple tasks.
     *
     * @param taskIds The business identifiers of the tasks
     * @return List of progress entries for all specified tasks
     */
    fun findByTaskIds(taskIds: List<String>): List<TaskProgress> =
        if (taskIds.isEmpty()) emptyList()
        else findIn("taskId", taskIds)

    /**
     * Find all progress entries with a specific action.
     *
     * @param action The action to filter by (e.g., "started", "checkpoint reached")
     * @return List of progress entries with the specified action
     */
    fun findByAction(action: String): List<TaskProgress> =
        find(listOf(EdgeFilter("action", Op.EQ, action)))
}
