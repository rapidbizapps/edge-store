package edgestore.example

import edgestore.BaseModel
import edgestore.Entity    // From edge-store, not io.objectbox!
import edgestore.Index

// ============================================================================
// Example Entities - ZERO ObjectBox Imports!
// Just import from `edgestore` package and extend BaseModel
// ============================================================================

/**
 * Represents a unit of work managed by the app.
 *
 * By extending [BaseModel], you get these fields automatically:
 * - `id: Long` - ObjectBox primary key (auto-assigned)
 * - `_id: String` - Business identifier (indexed)
 * - `createdAt: Long` - Creation timestamp
 * - `updatedAt: Long` - Last modification timestamp
 *
 * Notice: Only `@Entity` annotation needed, imported from `edgestore`!
 */
@Entity
class Task() : BaseModel() {
    var startTime: Long = 0
    var endTime: Long? = null

    @Index
    lateinit var status: String

    /**
     * Convenience constructor for creating Task instances.
     */
    constructor(
        _id: String,
        startTime: Long,
        endTime: Long? = null,
        status: String
    ) : this() {
        this._id = _id
        this.startTime = startTime
        this.endTime = endTime
        this.status = status
    }
}

/**
 * Captures a single update in a task's lifecycle.
 */
@Entity
class TaskProgress() : BaseModel() {
    @Index
    lateinit var taskId: String

    lateinit var action: String
    var timestamp: Long = 0

    constructor(
        _id: String,
        taskId: String,
        action: String,
        timestamp: Long
    ) : this() {
        this._id = _id
        this.taskId = taskId
        this.action = action
        this.timestamp = timestamp
    }
}

/**
 * Example project entity.
 */
@Entity
class Project() : BaseModel() {
    lateinit var name: String
    var description: String = ""

    constructor(_id: String, name: String, description: String = "") : this() {
        this._id = _id
        this.name = name
        this.description = description
    }
}

// ============================================================================
// Usage Examples
// ============================================================================

/*
// Creating entities
val task = Task(
    _id = "task-001",
    startTime = System.currentTimeMillis(),
    status = "running"
)

val progress = TaskProgress(
    _id = "progress-001",
    taskId = "task-001",
    action = "started",
    timestamp = System.currentTimeMillis()
)

// With ObjectBox BoxStore (in your main project)
val box = boxStore.boxFor(Task::class.java)
box.put(task)

val allTasks = box.all
val runningTasks = box.query()
    .equal(Task_.status, "running", QueryBuilder.StringOrder.CASE_SENSITIVE)
    .build()
    .find()
*/
