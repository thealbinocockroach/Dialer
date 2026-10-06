package com.example.repository

import com.example.data.local.CallLogDao
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallLogRepository(
    private val callLogDao: CallLogDao
) {
    val allCallLogs: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()

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

    suspend fun seedInitialLogsIfEmpty() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dummyLogs = listOf(
            CallLogEntity(
                number = "+1 (555) 019-2831",
                cachedName = "Alex Vance",
                callType = CallType.INCOMING,
                timestamp = now - 25 * 60 * 1000,
                durationSeconds = 142,
                isRecorded = true
            ),
            CallLogEntity(
                number = "+1 (555) 911-0000",
                cachedName = "Brutalist Dispatch",
                callType = CallType.OUTGOING,
                timestamp = now - 2 * 3600 * 1000,
                durationSeconds = 54,
                isRecorded = false
            ),
            CallLogEntity(
                number = "+1 (800) 555-SPAM",
                cachedName = "Unknown Telemarketer",
                callType = CallType.BLOCKED,
                timestamp = now - 5 * 3600 * 1000,
                durationSeconds = 0,
                isRecorded = false
            ),
            CallLogEntity(
                number = "+1 (555) 442-8921",
                cachedName = "Elena Rostova",
                callType = CallType.MISSED,
                timestamp = now - 9 * 3600 * 1000,
                durationSeconds = 0,
                isRecorded = false
            ),
            CallLogEntity(
                number = "+1 (555) 773-6102",
                cachedName = "Kai Tanaka",
                callType = CallType.OUTGOING,
                timestamp = now - 22 * 3600 * 1000,
                durationSeconds = 310,
                isRecorded = true
            ),
            CallLogEntity(
                number = "+1 (555) 604-9988",
                cachedName = "Zara O'Connor",
                callType = CallType.INCOMING,
                timestamp = now - 48 * 3600 * 1000,
                durationSeconds = 88,
                isRecorded = false
            )
        )
        // Check if logs are empty by checking first
        // We can safely insert if empty
        dummyLogs.forEach { callLogDao.insert(it) }
    }
}
