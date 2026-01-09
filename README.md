# EdgeStore

EdgeStore is an Android/Kotlin library that enforces a single data-access boundary independent of the underlying database. All CRUD work routes through the `EdgeStore` interface so application code never touches the storage layer directly, while still allowing model-agnostic storage, mutation tracking, and clear debuggability. The current wrapper is implemented for ObjectBox, with Couchbase Lite and Ditto work in progress as we evaluate which database sync service to adopt; the approach can be extended to a multi-wrapper that supports additional ORM libraries in the future.

## Highlights
- **EntityDao pattern** for type-safe, boilerplate-free queries
- Centralized CRUD contract via `EdgeStore` with structured filters and mutation contexts
- `Edge` singleton for simple initialization and session management
- Implementation layer validates `_id` presence, writes through `EdgeBox`, and records `EdgeDirty` entries to track local mutations
- Pluggable serialization with the default JSON serializer (`kotlinx.serialization`)
- EdgeStore annotations keep app models decoupled from ObjectBox

## Quick Start

### 1. Define Models and Entity Descriptors

Models must include a business `_id` field. Pair each model with an `EdgeEntity` descriptor:

```kotlin
@EdgeModel
@Serializable
data class Task(
    @EdgeId
    var id: Long = 0,       // ObjectBox internal ID
    val _id: String,        // Business identifier (required)
    val title: String,
    val status: String
)

object TaskEntity : EdgeEntity<Task> {
    override val name = "task"
    override val clazz = Task::class.java
}
```

### 2. Create DAO Objects

Extend `EntityDao` to create type-safe query interfaces with custom methods:

```kotlin
object Tasks : EntityDao<Task>(Task::class, TaskEntity) {

    fun findByStatus(status: String): List<Task> =
        find(listOf(EdgeFilter("status", Op.EQ, status)))

    fun findActive(): List<Task> = findByStatus("active")
}
```

### 3. Initialize Edge at App Startup

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Edge.init(
            context = this,
            config = EdgeStoreConfig(JsonSerializer()),
            defaultStoreName = "myapp"
        )
    }
}
```

### 4. Use DAOs in Your Code

```kotlin
// Clean, type-safe queries - no ObjectBox APIs exposed
val allTasks = Tasks.findAll()
val task = Tasks.findById("task-001")
val activeTasks = Tasks.findActive()
val exists = Tasks.exists("task-001")
val count = Tasks.count()
```

## EntityDao API Reference

### Built-in Methods

| Method | Description |
|--------|-------------|
| `findAll()` | Returns all entities of this type |
| `findById(remoteId)` | Find by business ID (`_id`), returns null if not found |
| `find(filters)` | Query with list of `EdgeFilter` conditions |
| `findIn(field, values)` | Find where field value is in the provided list |
| `exists(remoteId)` | Check if entity with given ID exists |
| `count()` | Count all entities of this type |

### Query Filters

```kotlin
// Equals
EdgeFilter("status", Op.EQ, "active")

// In list
EdgeFilter("status", Op.IN, listOf("active", "pending"))

// Greater than
EdgeFilter("createdAt", Op.GT, timestamp)

// Less than
EdgeFilter("priority", Op.LT, 5)

// Combine multiple filters (AND logic)
find(listOf(
    EdgeFilter("status", Op.EQ, "active"),
    EdgeFilter("priority", Op.GT, 3)
))
```

### Custom DAO Methods

Add domain-specific queries by extending `EntityDao`:

```kotlin
object TaskProgresses : EntityDao<TaskProgress>(TaskProgress::class, TaskProgressEntity) {

    fun findByTaskId(taskId: String): List<TaskProgress> =
        find(listOf(EdgeFilter("taskId", Op.EQ, taskId)))

    fun findByTaskIds(taskIds: List<String>): List<TaskProgress> =
        if (taskIds.isEmpty()) emptyList()
        else findIn("taskId", taskIds)

    fun findByAction(action: String): List<TaskProgress> =
        find(listOf(EdgeFilter("action", Op.EQ, action)))
}
```

## Create/Update/Delete Operations

For mutations, access the underlying store through the session:

```kotlin
class TaskRepository(private val serializer: JsonSerializer) {

    private val store get() = Edge.session().core.store()

    fun createTask(task: Task): String {
        val payload = serializer.serialize(task)
        return store.create(
            TaskEntity,
            payload,
            EdgeContext(source = "ui", actor = "user123")
        )
    }

    fun updateTask(task: Task) {
        val payload = serializer.serialize(task)
        store.update(TaskEntity, task._id, payload, EdgeContext(source = "ui"))
    }

    fun deleteTask(id: String) {
        store.delete(TaskEntity, id, EdgeContext(source = "ui"))
    }
}
```

### EdgeContext

Mutation context for tracking changes:

```kotlin
EdgeContext(
    source = "ui",          // Required: "ui", "sync", "p2p"
    actor = "user123",      // Optional: who performed the action
    reason = "user request" // Optional: why
)
```

## Alternative: Direct EdgeStore Usage

For more control, you can use `EdgeStoreInitializer` directly instead of the `Edge` singleton:

```kotlin
val edgeStoreInitializer = EdgeStoreInitializer(
    appContext,
    EdgeStoreConfig(JsonSerializer())
)
val taskStore = edgeStoreInitializer.getOrCreate(storeName = "task")

// Create
val taskPayload = JsonSerializer().serialize(task)
taskStore.create(TaskEntity, taskPayload, EdgeContext(source = "ui"))

// Query
val tasks: List<Task> = taskStore.query(TaskEntity, emptyList())
val filtered: List<Task> = taskStore.query(
    TaskEntity,
    listOf(EdgeFilter("status", Op.EQ, "running"))
)
```

## Multiple Stores

For apps with multiple databases:

```kotlin
// Initialize with default store
Edge.init(context, config, defaultStoreName = "main")

// Access different stores via named sessions
val mainSession = Edge.session()           // Uses "main"
val cacheSession = Edge.session("cache")   // Uses "cache"
```

## Shutdown

```kotlin
// Release resources when done
Edge.shutdown()

// Or with direct initializer usage:
edgeStoreInitializer.close(storeName)  // Single store
edgeStoreInitializer.closeAll()        // All stores
```

## Architecture

```
App Code (ViewModels, Repositories)
         │
         ▼
    EntityDao<T>  ←── Type-safe queries, custom methods
         │
         ▼
    Edge.session().core  ←── EdgeCore delegates to EdgeStore
         │
         ▼
    EdgeStore (interface)  ←── Public API boundary
         │
         ▼
    EdgeStoreImpl  ←── Validation, logging, dirty tracking
         │
         ▼
    EdgeBox  ←── ObjectBox wrapper (internal only)
         │
         ▼
    ObjectBox  ←── Storage engine (never exposed)
```

**Key Principle:** Application code never imports ObjectBox types. All persistence goes through `EdgeStore` and `EntityDao`.

## Building

Use Gradle with the Android and Kotlin plugins declared in [`build.gradle`](build.gradle):

```bash
./gradlew assemble        # Build library artifacts
./gradlew test            # Run unit tests
./gradlew compileDebugKotlin  # Compile only
```

## Examples

For a complete end-to-end demonstration, see:
- [`examples/TaskModels.kt`](examples/TaskModels.kt) - Entity and DAO definitions
- [`examples/ExampleUsage.kt`](examples/ExampleUsage.kt) - Full usage example

## Architecture Reference

The architectural guardrails and rationale for EdgeStore are documented in [`EdgeStore_Local_Persistence_Architecture.md`](EdgeStore_Local_Persistence_Architecture.md).
