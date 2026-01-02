package edgestore.example

import android.content.Context
import edgestore.EdgeContext
import edgestore.EdgeEntity
import edgestore.EdgeFilter
import edgestore.EdgeStore
import edgestore.EdgeStoreConfig
import edgestore.EdgeStoreInitializer
import edgestore.Op
import edgestore.util.JsonSerializer
import kotlinx.serialization.Serializable

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
     * Launches a demo flow that boots the store, saves a note entity, and
     * then queries it back to verify persistence.
     */
    fun run() {
        val edgeStore = edgeStoreInitializer.getOrCreate(storeName = "notes")

        val note = Note(
            _id = "note-001",
            title = "EdgeStore quickstart",
            body = "EdgeStore persists your entities via ObjectBox.",
            tags = listOf("quickstart", "edge-store")
        )

        saveNote(edgeStore, note)
        val savedNotes = queryQuickstartNotes(edgeStore)

        if (savedNotes.isNotEmpty()) {
            println("Saved note: ${'$'}{savedNotes.first().title}")
        }
    }

    private fun saveNote(edgeStore: EdgeStore, note: Note) {
        val payload = serializer.serialize(note)
        edgeStore.create(
            NoteEntity,
            payload,
            EdgeContext(source = "example", actor = "demo-user", reason = "first run")
        )
    }

    private fun queryQuickstartNotes(edgeStore: EdgeStore): List<Note> {
        return edgeStore.query(
            NoteEntity,
            listOf(EdgeFilter(field = "tags", op = Op.IN, value = "quickstart"))
        )
    }
}

/**
 * Serializable domain model with the required business identifier [_id].
 */
@Serializable
data class Note(
    val _id: String,
    val title: String,
    val body: String,
    val tags: List<String>
)

/**
 * EdgeEntity descriptor for the Note model so EdgeStore can manage it.
 */
object NoteEntity : EdgeEntity<Note> {
    override val name: String = "note"
    override val clazz: Class<Note> = Note::class.java
}
