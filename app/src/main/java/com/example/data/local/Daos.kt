package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE speedDialDigit IS NOT NULL ORDER BY speedDialDigit ASC")
    fun getSpeedDialContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE speedDialDigit = :digit LIMIT 1")
    suspend fun getBySpeedDialDigit(digit: Int): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY callCount DESC, lastContactedTimestamp DESC LIMIT :limit")
    fun getFrequentlyContacted(limit: Int = 8): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalizedNumber OR phoneNumber = :number LIMIT 1")
    suspend fun findByNumber(normalizedNumber: String, number: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("UPDATE contacts SET callCount = callCount + 1, lastContactedTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementCallCount(id: Long, timestamp: Long)

    @Query("UPDATE contacts SET speedDialDigit = NULL WHERE speedDialDigit = :digit")
    suspend fun clearSpeedDialDigit(digit: Int)

    @Query("UPDATE contacts SET speedDialDigit = :digit WHERE id = :id")
    suspend fun setSpeedDialDigit(id: Long, digit: Int)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getCount(): Int

    @Query("DELETE FROM contacts")
    suspend fun clearAll()

    @Query("DELETE FROM contacts WHERE lookupKey = '' OR lookupKey IS NULL OR displayName IN ('Alex Vance', 'Brutalist Dispatch', 'Elena Rostova', 'Kai Tanaka', 'Marcus Brody', 'Nova Sterling', 'Samir Patel', 'Zara O''Connor')")
    suspend fun deleteMockContacts()
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE callType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: CallType): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLogEntity): Long

    @Update
    suspend fun update(callLog: CallLogEntity)

    @Delete
    suspend fun delete(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Query("DELETE FROM call_logs WHERE cachedName IN ('Alex Vance', 'Brutalist Dispatch', 'Unknown Telemarketer', 'Elena Rostova', 'Kai Tanaka', 'Zara O''Connor', 'Marcus Brody', 'Nova Sterling', 'Samir Patel') OR number LIKE '%555-01%' OR number LIKE '%55501%'")
    suspend fun deleteMockLogs()
}

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: CallRecordingEntity): Long

    @Delete
    suspend fun delete(recording: CallRecordingEntity)

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    suspend fun getById(id: Long): CallRecordingEntity?
}

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_rules ORDER BY createdAt DESC")
    fun getAllBlocked(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_rules")
    suspend fun getAllBlockedList(): List<BlockedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blocked: BlockedNumberEntity): Long

    @Delete
    suspend fun delete(blocked: BlockedNumberEntity)

    @Query("UPDATE blocked_rules SET blockedCount = blockedCount + 1 WHERE id = :id")
    suspend fun incrementBlockedCount(id: Long)
}

@Dao
interface QuickResponseDao {
    @Query("SELECT * FROM quick_responses ORDER BY id ASC")
    fun getAll(): Flow<List<QuickResponseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(response: QuickResponseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(responses: List<QuickResponseEntity>)

    @Delete
    suspend fun delete(response: QuickResponseEntity)

    @Query("SELECT COUNT(*) FROM quick_responses")
    suspend fun getCount(): Int
}
