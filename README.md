# EdgeStore

EdgeStore is an Android/Kotlin library that enforces a single data-access boundary independent of the underlying database. All CRUD work routes through the `EdgeStore` interface so application code never touches the storage layer directly, while still allowing model-agnostic storage, mutation tracking, and clear debuggability. The current wrapper is implemented for ObjectBox, with Couchbase Lite and Ditto work in progress as we evaluate which database sync service to adopt; the approach can be extended to a multi-wrapper that supports additional ORM libraries in the future.

## Highlights
- Centralized CRUD contract via `EdgeStore` with structured filters and mutation contexts.
- `EdgeStoreInitializer` owns BoxStore bootstrapping and caches one `EdgeStore` per local store name.
- Implementation layer validates `_id` presence, writes through `EdgeBox`, and records `EdgeDirty` entries to track local mutations.
- Pluggable serialization with the default JSON serializer (`kotlinx.serialization`).
- EdgeStore annotations keep app models decoupled from ObjectBox.

## Usage
1. **Define your models and entity descriptors.** Models must include a business `_id` field. Pair each model with an `EdgeEntity` descriptor so the store can map payloads to types.
   ```kotlin
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

   object TaskEntity : EdgeEntity<Task> {
       override val name = "task"
       override val clazz = Task::class.java
   }
   ```
2. **Initialize an `EdgeStore` for a named store.** The initializer builds or reuses an ObjectBox database under `<app files dir>/objectbox/<storeName>` and hands back a cached store instance.
   ```kotlin
   val edgeStoreInitializer = EdgeStoreInitializer(
       appContext,
       EdgeStoreConfig(JsonSerializer())
   )
   val taskStore = edgeStoreInitializer.getOrCreate(
       storeName = "task",
       engine = EdgeStoreEngine.OBJECTBOX
   )
   ```
3. **Perform CRUD through the store.** Serialize your models, call `create`/`update`/`delete`, and issue structured `query` calls.
   ```kotlin
   // Create
   val taskPayload = JsonSerializer().serialize(task)
   taskStore.create(TaskEntity, taskPayload, EdgeContext(source = "ui"))

   // Query
   val tasks = taskStore.query(TaskEntity, emptyList())
   ```
4. **Shut down stores when finished.** Call `close(storeName)` to release a single store or `closeAll()` to stop every cached store.

For a fuller end-to-end demonstration, see [`examples/ExampleUsage.kt`](examples/ExampleUsage.kt).

## Building
Use Gradle with the Android and Kotlin plugins declared in [`build.gradle`](build.gradle). Typical commands include:
- `gradle assemble` to build the library artifacts.
- `gradle test` for unit tests (none are defined yet, so this is a placeholder for future coverage).

## Architecture reference
The architectural guardrails and rationale for EdgeStore are documented in [`EdgeStore_Local_Persistence_Architecture.md`](EdgeStore_Local_Persistence_Architecture.md).
