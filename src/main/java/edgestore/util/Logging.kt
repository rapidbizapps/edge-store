package edgestore.util

import android.util.Log

/**
 * Logging utilities for EdgeStore operations to maximize debuggability.
 */
object EdgeLogger {

    private const val TAG = "EdgeStore"

    fun logCreate(entity: EdgeEntity, _id: String, ctx: EdgeContext) {
        Log.d(TAG, "CREATE: ${entity.name} with _id=$_id, source=${ctx.source}, actor=${ctx.actor}, reason=${ctx.reason}")
    }

    fun logUpdate(entity: EdgeEntity, _id: String, ctx: EdgeContext) {
        Log.d(TAG, "UPDATE: ${entity.name} with _id=$_id, source=${ctx.source}, actor=${ctx.actor}, reason=${ctx.reason}")
    }

    fun logDelete(entity: EdgeEntity, _id: String, ctx: EdgeContext) {
        Log.d(TAG, "DELETE: ${entity.name} with _id=$_id, source=${ctx.source}, actor=${ctx.actor}, reason=${ctx.reason}")
    }

    fun logQuery(entity: EdgeEntity, filters: List<EdgeFilter>) {
        Log.d(TAG, "QUERY: ${entity.name} with filters=$filters")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
