package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_logs",
    indices = [
        Index(value = ["number"]),
        Index(value = ["timestamp"])
    ]
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val cachedName: String = "",
    val callType: CallType, // INCOMING, OUTGOING, MISSED, BLOCKED
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isRecorded: Boolean = false,
    val recordingId: Long? = null,
    val recordingPath: String? = null
)
