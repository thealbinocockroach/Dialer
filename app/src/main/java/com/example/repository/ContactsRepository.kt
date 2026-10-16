package com.example.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.local.ContactDao
import com.example.data.model.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val context: Context,
    private val contactDao: ContactDao
) {
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
        .map { list -> list.distinctBy { it.displayName.trim().lowercase() } }

    val frequentlyContacted: Flow<List<ContactEntity>> = contactDao.getFrequentlyContacted(8)
        .map { list -> list.distinctBy { it.displayName.trim().lowercase() } }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun recordCallInteraction(phoneNumber: String, contactName: String = "") {
        val clean = phoneNumber.filter { it.isDigit() }
        val existing = contactDao.findByNumber(clean, phoneNumber)
        if (existing != null) {
            contactDao.incrementCallCount(existing.id, System.currentTimeMillis())
        }
    }

    suspend fun saveContact(contact: ContactEntity): Long {
        return if (contact.id == 0L) {
            contactDao.insert(contact)
        } else {
            contactDao.update(contact)
            contact.id
        }
    }

    suspend fun deleteContact(contact: ContactEntity) {
        contactDao.delete(contact)
    }

    suspend fun clearMockContactsIfAny() = withContext(Dispatchers.IO) {
        contactDao.deleteMockContacts()
        contactDao.deleteDuplicateContacts()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        contactDao.clearAll()
    }

    suspend fun purgeDuplicateContacts() = withContext(Dispatchers.IO) {
        contactDao.deleteDuplicateContacts()
    }

    suspend fun syncDeviceContacts(): Int = withContext(Dispatchers.IO) {
        try {
            if (!hasContactsPermission()) return@withContext 0
            val resolver: ContentResolver = context.contentResolver
            val cursor = try {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED
                    ),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )
            } catch (e: Throwable) {
                android.util.Log.e("ContactsRepository", "Failed to query ContactsProvider: ${e.message}", e)
                null
            }

            var count = 0
            cursor?.use {
                val contactsMap = linkedMapOf<String, ContactEntity>()
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val keyCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

                val colors = listOf("#FFE600", "#0055FF", "#00E676", "#FF2A85", "#FF0055", "#8B5CF6", "#00E5FF")
                var idx = 0

                while (it.moveToNext() && contactsMap.size < 2500) {
                    try {
                        val rawName = if (nameCol >= 0) it.getString(nameCol) ?: "Unknown" else "Unknown"
                        val name = rawName.trim().ifEmpty { "Unknown" }
                        val number = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                        val isStarred = if (starCol >= 0) {
                            try { it.getInt(starCol) == 1 } catch (_: Throwable) { false }
                        } else false
                        val key = if (keyCol >= 0) it.getString(keyCol) ?: "" else ""

                        if (number.isNotBlank()) {
                            val clean = number.filter { ch -> ch.isDigit() }
                            val nameKey = name.lowercase()

                            // Ensure strictly only ONE contact entry per person name!
                            if (!contactsMap.containsKey(nameKey)) {
                                contactsMap[nameKey] = ContactEntity(
                                    lookupKey = key.ifEmpty { "k_${idx}_${clean}" },
                                    displayName = name,
                                    phoneNumber = number,
                                    normalizedNumber = clean,
                                    isStarred = isStarred,
                                    colorHex = colors[idx % colors.size]
                                )
                                idx++
                            } else {
                                // If multiple numbers exist for this person, retain the primary and star status
                                val existing = contactsMap[nameKey]!!
                                if (isStarred && !existing.isStarred) {
                                    contactsMap[nameKey] = existing.copy(isStarred = true)
                                }
                            }
                        }
                    } catch (rowEx: Throwable) {
                        // Skip corrupted or unreadable contact row
                    }
                }

                val contactsList = contactsMap.values.toList()
                if (contactsList.isNotEmpty()) {
                    contactDao.clearAll() // Clear old/duplicate contacts so they don't multiply on each sync
                    contactDao.insertAll(contactsList)
                    contactDao.deleteDuplicateContacts()
                    count = contactsList.size
                }
            }
            count
        } catch (t: Throwable) {
            android.util.Log.e("ContactsRepository", "Error in syncDeviceContacts: ${t.message}", t)
            0
        }
    }
}
