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
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val context: Context,
    private val contactDao: ContactDao
) {
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val frequentlyContacted: Flow<List<ContactEntity>> = contactDao.getFrequentlyContacted(8)

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
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        contactDao.clearAll()
    }

    suspend fun syncDeviceContacts(): Int = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext 0
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(
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

        var count = 0
        cursor?.use {
            val contactsList = mutableListOf<ContactEntity>()
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            val keyCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

            val colors = listOf("#FFE600", "#0055FF", "#00E676", "#FF2A85", "#FF0055", "#8B5CF6", "#00E5FF")
            var idx = 0

            while (it.moveToNext()) {
                val name = if (nameCol >= 0) it.getString(nameCol) ?: "Unknown" else "Unknown"
                val number = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                val isStarred = if (starCol >= 0) it.getInt(starCol) == 1 else false
                val key = if (keyCol >= 0) it.getString(keyCol) ?: "" else ""

                if (number.isNotBlank()) {
                    val clean = number.filter { ch -> ch.isDigit() }
                    contactsList.add(
                        ContactEntity(
                            lookupKey = key,
                            displayName = name,
                            phoneNumber = number,
                            normalizedNumber = clean,
                            isStarred = isStarred,
                            colorHex = colors[idx % colors.size]
                        )
                    )
                    idx++
                }
            }
            if (contactsList.isNotEmpty()) {
                contactDao.insertAll(contactsList)
                count = contactsList.size
            }
        }
        count
    }
}
