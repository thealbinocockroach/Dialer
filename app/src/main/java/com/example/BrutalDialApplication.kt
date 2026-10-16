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
        try {
            // Safety: Catch unexpected background thread exceptions so app doesn't crash on budget/Go OEM devices
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                android.util.Log.e("BrutalDialApplication", "Uncaught exception intercepted on thread: ${thread.name}", throwable)
                // If it's a fatal UI thread crash, allow normal handling, otherwise log and prevent abrupt process death
                if (thread == android.os.Looper.getMainLooper().thread) {
                    previousHandler?.uncaughtException(thread, throwable)
                }
            }

            database = AppDatabase.getDatabase(this)
            preferences = SettingsPreferences(this)
            telephonyService = TelephonyService(this)

            contactsRepository = ContactsRepository(this, database.contactDao())
            callLogRepository = CallLogRepository(this, database.callLogDao())
            callBlockerService = CallBlockerService(database.blockedNumberDao(), preferences)
            callRecorderService = CallRecorderService(this, database.callRecordingDao())
            quickResponsesRepository = QuickResponsesRepository(database.quickResponseDao())

            // Background startup sync with exception handler so startup failure never crashes app
            val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
                android.util.Log.e("BrutalDialApplication", "Startup background error: ${throwable.message}", throwable)
            }
            CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
                try {
                    database.contactDao().deleteMockContacts()
                    database.contactDao().deleteDuplicateContacts()
                    database.callLogDao().deleteMockLogs()
                    if (contactsRepository.hasContactsPermission()) {
                        contactsRepository.syncDeviceContacts()
                    }
                    if (callLogRepository.hasCallLogPermission()) {
                        callLogRepository.syncDeviceCallLogs()
                    }
                    quickResponsesRepository.seedDefaultsIfEmpty()
                } catch (t: Throwable) {
                    android.util.Log.e("BrutalDialApplication", "Safe startup sync caught exception: ${t.message}", t)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("BrutalDialApplication", "Fatal error during Application.onCreate", t)
        }
    }
}
