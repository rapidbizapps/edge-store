package edgestore.example

import android.content.Context
import edgestore.Edge
import edgestore.EdgeContext
import edgestore.EdgeStoreConfig
import edgestore.util.JsonSerializer

/**
 * Minimal, self-contained example showing how an Android app can initialize
 * EdgeStore and use the EntityDao pattern for type-safe queries.
 *
 * This demonstrates the new DAO pattern where:
 * - Edge.init() initializes the store once at app startup
 * - Tasks and TaskProgresses DAOs provide type-safe query methods
 * - No ObjectBox types leak into application code
 */
class ExampleUsage(private val appContext: Context) {

    private val serializer = JsonSerializer()

    /**
     * Launches a demo flow that boots the store, saves a task entity plus
     * progress events, and then queries them back using the DAO pattern.
     */
    fun run() {
        // Initialize Edge once at app startup
        // This replaces the manual EdgeStoreInitializer creation
        Edge.init(
            context = appContext,
            config = EdgeStoreConfig(serializer),
            defaultStoreName = "task"
        )

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

        // Save entities using the store directly
        saveTask(task)
        taskEvents.forEach { saveTaskProgress(it) }

        // Query using the DAO pattern - type-safe and clean!
        val savedTasks = Tasks.findAll()
        val savedTaskProgress = TaskProgresses.findByTaskId(task._id)

        savedTasks.forEach { savedTask ->
            println("Saved task: id=${savedTask._id}, status=${savedTask.status}")
        }

        if (savedTaskProgress.isNotEmpty()) {
            println("Task progress for ${task._id}:")
            savedTaskProgress.forEach { progress ->
                println("- ${progress.action} at ${progress.timestamp}")
            }
        }

        // Demonstrate other DAO methods
        demonstrateDaoMethods(task._id)
    }

    /**
     * Demonstrates various DAO query methods.
     */
    private fun demonstrateDaoMethods(taskId: String) {
        // Find by ID
        val taskById = Tasks.findById(taskId)
        println("Found task by ID: ${taskById?._id}")

        // Find running tasks
        val runningTasks = Tasks.findRunning()
        println("Running tasks: ${runningTasks.size}")

        // Check if task exists
        val exists = Tasks.exists(taskId)
        println("Task exists: $exists")

        // Count all tasks
        val taskCount = Tasks.count()
        println("Total tasks: $taskCount")

        // Find progress for multiple tasks
        val progressForTasks = TaskProgresses.findByTaskIds(listOf(taskId))
        println("Progress entries for tasks: ${progressForTasks.size}")
    }

    private fun saveTask(task: Task) {
        val payload = serializer.serialize(task)
        Edge.session().core.store().create(
            TaskEntity,
            payload,
            EdgeContext(source = "example", actor = "demo-user", reason = "task demo")
        )
    }

    private fun saveTaskProgress(taskProgress: TaskProgress) {
        val payload = serializer.serialize(taskProgress)
        Edge.session().core.store().create(
            TaskProgressEntity,
            payload,
            EdgeContext(source = "example", actor = "demo-user", reason = "task demo")
        )
    }

    /**
     * Clean up resources when done.
     */
    fun cleanup() {
        Edge.shutdown()
    }
}
