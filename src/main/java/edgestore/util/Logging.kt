package edgestore.util

import android.util.Log

/**
 * Logging utilities for EdgeStore operations to maximize debuggability.
 */
object EdgeLogger {

    private const val TAG = "EdgeStore"

    fun logCreate(entityClass: Class<*>, _id: String) {
        Log.d(TAG, "CREATE: ${entityClass.simpleName} with _id=$_id")
    }

    fun logUpdate(entityClass: Class<*>, _id: String) {
        Log.d(TAG, "UPDATE: ${entityClass.simpleName} with _id=$_id")
    }

    fun logDelete(entityClass: Class<*>, _id: String) {
        Log.d(TAG, "DELETE: ${entityClass.simpleName} with _id=$_id")
    }

    fun logQuery(entityClass: Class<*>, filters: Map<String, Any>) {
        Log.d(TAG, "QUERY: ${entityClass.simpleName} with filters=$filters")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
