package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerNumber: String,
    val callerName: String = "",
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val fileSizeBytes: Long = 0L
)
