package edgestore.example

import android.content.Context
import edgestore.EdgeContext
import edgestore.EdgeFilter
import edgestore.EdgeStore
import edgestore.EdgeStoreConfig
import edgestore.EdgeStoreInitializer
import edgestore.Op
import edgestore.util.JsonSerializer

/**
 * Minimal, self-contained example showing how an Android app can initialize
 * EdgeStore and persist data into a local ObjectBox store.
 */
class ExampleUsage(private val appContext: Context) {

    private val serializer = JsonSerializer()
    private val edgeStoreInitializer = EdgeStoreInitializer(
        appContext,
        EdgeStoreConfig(serializer)
    )

    /**
     * Launches a demo flow that boots the store, saves a task entity plus
     * progress events, and then queries them back to verify persistence.
     */
    fun run() {
        // Creates or reuses a local ObjectBox-backed store under app files/objectbox/task
        // and returns an EdgeStore facade for it. The store name matches the collection
        // name defined on TaskEntity ("task").
        val taskStore = edgeStoreInitializer.getOrCreate(storeName = "task")

        val task = Task(
            _id = "task-001",
            startTime = System.currentTimeMillis(),
            endTime = null,
            status = "running"
        )

        val taskEvents = listOf(
            TaskProgress(
                _id = "progress-001",
                taskId = task._id,
                action = "started",
                timestamp = task.startTime
            ),
            TaskProgress(
                _id = "progress-002",
                taskId = task._id,
                action = "checkpoint reached",
                timestamp = task.startTime + 5000
            )
        )

        saveTask(taskStore, task)
        taskEvents.forEach { saveTaskProgress(taskStore, it) }
        val savedTasks = queryAllTasks(taskStore)
        val savedTaskProgress = queryTaskProgress(taskStore, task._id)

        savedTasks.forEach { savedTask ->
            println("Saved task: id=${'$'}{savedTask._id}, status=${'$'}{savedTask.status}")
        }

        if (savedTaskProgress.isNotEmpty()) {
            println("Task progress for ${'$'}{task._id}:")
            savedTaskProgress.forEach { progress ->
                println("- ${'$'}{progress.action} at ${'$'}{progress.timestamp}")
            }
        }
    }

    private fun saveTask(edgeStore: EdgeStore, task: Task) {
        val payload = serializer.serialize(task)
        edgeStore.create(
            TaskEntity,
            payload,
            EdgeContext(source = "example", actor = "demo-user", reason = "task demo")
        )
    }

    private fun saveTaskProgress(edgeStore: EdgeStore, taskProgress: TaskProgress) {
        val payload = serializer.serialize(taskProgress)
        edgeStore.create(
            TaskProgressEntity,
            payload,
            EdgeContext(source = "example", actor = "demo-user", reason = "task demo")
        )
    }

    private fun queryAllTasks(edgeStore: EdgeStore): List<Task> {
        return edgeStore.query(
            TaskEntity,
            emptyList()
        )
    }

    private fun queryTaskProgress(edgeStore: EdgeStore, taskId: String): List<TaskProgress> {
        return edgeStore.query(
            TaskProgressEntity,
            listOf(EdgeFilter(field = "taskId", op = Op.EQ, value = taskId))
        )
    }
}
