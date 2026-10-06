package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_rules")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String, // Exact phone number or prefix e.g. "+1800"
    val isPrefixPattern: Boolean = false,
    val note: String = "",
    val blockedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_responses")
data class QuickResponseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isDefault: Boolean = false
)
