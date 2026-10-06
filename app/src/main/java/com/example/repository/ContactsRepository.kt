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
    val speedDialContacts: Flow<List<ContactEntity>> = contactDao.getSpeedDialContacts()

    suspend fun getBySpeedDial(digit: Int): ContactEntity? {
        return contactDao.getBySpeedDialDigit(digit)
    }

    suspend fun setSpeedDial(contactId: Long, digit: Int) {
        // Clear any contact currently holding this digit
        contactDao.clearSpeedDialDigit(digit)
        contactDao.setSpeedDialDigit(contactId, digit)
    }

    suspend fun clearSpeedDial(digit: Int) {
        contactDao.clearSpeedDialDigit(digit)
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

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = contactDao.getCount()
            if (count == 0) {
                // If device permissions granted, sync; otherwise populate high-contrast starter contacts
                if (hasContactsPermission()) {
                    syncDeviceContacts()
                }
                // If still empty or no permission, seed curated contacts
                if (contactDao.getCount() == 0) {
                    val curated = listOf(
                        ContactEntity(
                            displayName = "Alex Vance",
                            phoneNumber = "+1 (555) 019-2831",
                            normalizedNumber = "15550192831",
                            email = "alex.vance@blackmesa.org",
                            isStarred = true,
                            callCount = 18,
                            lastContactedTimestamp = System.currentTimeMillis() - 3600000,
                            speedDialDigit = 2,
                            colorHex = "#FFE600"
                        ),
                        ContactEntity(
                            displayName = "Brutalist Dispatch",
                            phoneNumber = "+1 (555) 911-0000",
                            normalizedNumber = "15559110000",
                            email = "dispatch@brutal.local",
                            isStarred = true,
                            callCount = 25,
                            lastContactedTimestamp = System.currentTimeMillis() - 7200000,
                            speedDialDigit = 3,
                            colorHex = "#0055FF"
                        ),
                        ContactEntity(
                            displayName = "Elena Rostova",
                            phoneNumber = "+1 (555) 442-8921",
                            normalizedNumber = "15554428921",
                            email = "elena.r@design.studio",
                            isStarred = false,
                            callCount = 9,
                            lastContactedTimestamp = System.currentTimeMillis() - 14400000,
                            speedDialDigit = 4,
                            colorHex = "#FF2A85"
                        ),
                        ContactEntity(
                            displayName = "Kai Tanaka",
                            phoneNumber = "+1 (555) 773-6102",
                            normalizedNumber = "15557736102",
                            email = "kai@cybersec.sh",
                            isStarred = true,
                            callCount = 14,
                            lastContactedTimestamp = System.currentTimeMillis() - 86400000,
                            speedDialDigit = 5,
                            colorHex = "#00E676"
                        ),
                        ContactEntity(
                            displayName = "Marcus Brody",
                            phoneNumber = "+1 (555) 234-5678",
                            normalizedNumber = "15552345678",
                            email = "m.brody@museum.edu",
                            isStarred = false,
                            callCount = 4,
                            lastContactedTimestamp = System.currentTimeMillis() - 172800000,
                            speedDialDigit = 6,
                            colorHex = "#8B5CF6"
                        ),
                        ContactEntity(
                            displayName = "Nova Sterling",
                            phoneNumber = "+1 (555) 889-1120",
                            normalizedNumber = "15558891120",
                            email = "nova@audio-ops.fm",
                            isStarred = false,
                            callCount = 6,
                            lastContactedTimestamp = System.currentTimeMillis() - 259200000,
                            speedDialDigit = 7,
                            colorHex = "#FF6D00"
                        ),
                        ContactEntity(
                            displayName = "Samir Patel",
                            phoneNumber = "+1 (555) 301-7744",
                            normalizedNumber = "15553017744",
                            email = "samir.p@hardware.io",
                            isStarred = false,
                            callCount = 3,
                            lastContactedTimestamp = System.currentTimeMillis() - 345600000,
                            speedDialDigit = 8,
                            colorHex = "#00E5FF"
                        ),
                        ContactEntity(
                            displayName = "Zara O'Connor",
                            phoneNumber = "+1 (555) 604-9988",
                            normalizedNumber = "15556049988",
                            email = "zara@signal.net",
                            isStarred = true,
                            callCount = 12,
                            lastContactedTimestamp = System.currentTimeMillis() - 400000000,
                            speedDialDigit = 9,
                            colorHex = "#FF0055"
                        )
                    )
                    contactDao.insertAll(curated)
                }
            }
        }
    }

    suspend fun syncDeviceContacts() = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext
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
            }
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
