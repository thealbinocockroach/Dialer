package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsPreferences
import com.example.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrutalDialApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: SettingsPreferences
        private set

    lateinit var contactsRepository: ContactsRepository
        private set

    lateinit var callLogRepository: CallLogRepository
        private set

    lateinit var callBlockerService: CallBlockerService
        private set

    lateinit var callRecorderService: CallRecorderService
        private set

    lateinit var quickResponsesRepository: QuickResponsesRepository
        private set

    lateinit var telephonyService: TelephonyService
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        preferences = SettingsPreferences(this)
        telephonyService = TelephonyService(this)

        contactsRepository = ContactsRepository(this, database.contactDao())
        callLogRepository = CallLogRepository(database.callLogDao())
        callBlockerService = CallBlockerService(database.blockedNumberDao(), preferences)
        callRecorderService = CallRecorderService(this, database.callRecordingDao())
        quickResponsesRepository = QuickResponsesRepository(database.quickResponseDao())

        // Seed data in background
        CoroutineScope(Dispatchers.IO).launch {
            contactsRepository.seedInitialDataIfEmpty()
            callLogRepository.seedInitialLogsIfEmpty()
            callBlockerService.seedInitialBlockedIfEmpty()
            quickResponsesRepository.seedDefaultsIfEmpty()
        }
    }
}
