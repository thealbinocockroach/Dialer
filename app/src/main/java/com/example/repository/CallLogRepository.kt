package com.example.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.data.local.CallLogDao
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallLogRepository(
    private val context: Context,
    private val callLogDao: CallLogDao
) {
    val allCallLogs: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()

    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLogsByType(type: CallType): Flow<List<CallLogEntity>> {
        return callLogDao.getLogsByType(type)
    }

    suspend fun addLog(
        number: String,
        cachedName: String,
        callType: CallType,
        durationSeconds: Int = 0,
        isRecorded: Boolean = false,
        recordingId: Long? = null,
        recordingPath: String? = null
    ): Long {
        val entity = CallLogEntity(
            number = number,
            cachedName = cachedName,
            callType = callType,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            isRecorded = isRecorded,
            recordingId = recordingId,
            recordingPath = recordingPath
        )
        return callLogDao.insert(entity)
    }

    suspend fun deleteLog(log: CallLogEntity) {
        callLogDao.delete(log)
    }

    suspend fun clearAll() {
        callLogDao.clearAll()
    }

    suspend fun syncDeviceCallLogs(): Int = withContext(Dispatchers.IO) {
        try {
            if (!hasCallLogPermission()) return@withContext 0

            val resolver: ContentResolver = context.contentResolver
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            val cursor = try {
                resolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )
            } catch (e: Throwable) {
                android.util.Log.e("CallLogRepository", "Failed to query CallLog Provider: ${e.message}", e)
                null
            }

            var synced = 0
            cursor?.use {
                val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
                val durCol = it.getColumnIndex(CallLog.Calls.DURATION)

                val logs = mutableListOf<CallLogEntity>()
                while (it.moveToNext() && logs.size < 500) {
                    try {
                        val num = if (numCol >= 0) it.getString(numCol) ?: "Unknown" else "Unknown"
                        val name = if (nameCol >= 0) it.getString(nameCol) ?: "" else ""
                        val typeInt = if (typeCol >= 0) {
                            try { it.getInt(typeCol) } catch (_: Throwable) { CallLog.Calls.INCOMING_TYPE }
                        } else CallLog.Calls.INCOMING_TYPE
                        val date = if (dateCol >= 0) {
                            try { it.getLong(dateCol) } catch (_: Throwable) { System.currentTimeMillis() }
                        } else System.currentTimeMillis()
                        val dur = if (durCol >= 0) {
                            try { it.getInt(durCol) } catch (_: Throwable) { 0 }
                        } else 0

                        val callType = when (typeInt) {
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            CallLog.Calls.BLOCKED_TYPE, CallLog.Calls.REJECTED_TYPE -> CallType.BLOCKED
                            else -> CallType.INCOMING
                        }

                        logs.add(
                            CallLogEntity(
                                number = num,
                                cachedName = name,
                                callType = callType,
                                timestamp = date,
                                durationSeconds = dur
                            )
                        )
                    } catch (rowEx: Throwable) {
                        // Skip corrupted log row
                    }
                }

                if (logs.isNotEmpty()) {
                    callLogDao.clearAll() // replace with real logs
                    callLogDao.insertAll(logs)
                    synced = logs.size
                }
            }
            synced
        } catch (t: Throwable) {
            android.util.Log.e("CallLogRepository", "Error in syncDeviceCallLogs: ${t.message}", t)
            0
        }
    }
}
