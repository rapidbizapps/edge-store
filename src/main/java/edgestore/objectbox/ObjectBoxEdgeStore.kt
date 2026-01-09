package edgestore.objectbox

import android.content.Context
import edgestore.EdgeBox
import edgestore.EdgeStore
import edgestore.EdgeStoreConfig
import edgestore.EdgeStoreImpl
import edgestore.MyObjectBox
import java.io.File

/**
 * ObjectBox-backed EdgeStore implementation.
 */
internal object ObjectBoxEdgeStore {

    fun create(
        context: Context,
        storeName: String,
        config: EdgeStoreConfig = EdgeStoreConfig()
    ): EdgeStore {
        val appContext = context.applicationContext
        val dbDir = File(appContext.filesDir, "objectbox/$storeName")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        val boxStore = MyObjectBox.builder()
            .androidContext(appContext)
            .directory(dbDir)
            .build()

        val edgeBox = EdgeBox(boxStore)
        return EdgeStoreImpl(edgeBox, config)
    }
}
