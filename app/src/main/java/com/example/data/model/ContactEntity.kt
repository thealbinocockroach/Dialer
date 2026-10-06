package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["phoneNumber"], unique = false),
        Index(value = ["normalizedNumber"], unique = false),
        Index(value = ["displayName"], unique = false),
        Index(value = ["speedDialDigit"], unique = false)
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lookupKey: String = "",
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String = "",
    val email: String = "",
    val isStarred: Boolean = false,
    val callCount: Int = 0,
    val lastContactedTimestamp: Long = 0L,
    val speedDialDigit: Int? = null, // Digits 2 to 99
    val isAutoRecord: Boolean = false,
    val colorHex: String = "#FFE600"
)
