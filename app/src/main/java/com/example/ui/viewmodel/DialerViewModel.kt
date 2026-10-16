package com.example.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BrutalDialApplication
import com.example.data.local.SettingsPreferences
import com.example.data.model.*
import com.example.repository.*
import com.example.t9.T9MatchResult
import com.example.t9.T9SearchEngine
import com.example.telecom.BrutalInCallService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DialerTab {
    RECENTS,
    KEYPAD,
    CONTACTS
}

data class UssdSession(
    val code: String,
    val isRunning: Boolean,
    val responseText: String? = null
)

class DialerViewModel(
    private val contactsRepository: ContactsRepository,
    private val callLogRepository: CallLogRepository,
    private val callBlockerService: CallBlockerService,
    private val callRecorderService: CallRecorderService,
    private val quickResponsesRepository: QuickResponsesRepository,
    private val telephonyService: TelephonyService,
    val preferences: SettingsPreferences
) : ViewModel() {

    private val _currentTab = MutableStateFlow(DialerTab.RECENTS)
    val currentTab: StateFlow<DialerTab> = _currentTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _dialedInput = MutableStateFlow("")
    val dialedInput: StateFlow<String> = _dialedInput.asStateFlow()

    private val _hasCallLogPermission = MutableStateFlow(callLogRepository.hasCallLogPermission())
    val hasCallLogPermission: StateFlow<Boolean> = _hasCallLogPermission.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(contactsRepository.hasContactsPermission())
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _isDefaultDialer = MutableStateFlow(telephonyService.isDefaultDialer())
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _canDrawOverlays = MutableStateFlow(telephonyService.canDrawOverlays())
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays.asStateFlow()

    private val _ussdSession = MutableStateFlow<UssdSession?>(null)
    val ussdSession: StateFlow<UssdSession?> = _ussdSession.asStateFlow()

    fun dismissUssd() {
        _ussdSession.value = null
    }

    val callDisplayMode = preferences.callDisplayMode
    fun setCallDisplayMode(mode: OverlayMode) = preferences.setCallDisplayMode(mode)

    val miniPopupKeepAfterAnswer = preferences.miniPopupKeepAfterAnswer
    fun setMiniPopupKeepAfterAnswer(value: Boolean) = preferences.setMiniPopupKeepAfterAnswer(value)

    val miniPopupAutoMinimize = preferences.miniPopupAutoMinimize
    fun setMiniPopupAutoMinimize(value: Boolean) = preferences.setMiniPopupAutoMinimize(value)

    val miniPopupShowAvatar = preferences.miniPopupShowAvatar
    fun setMiniPopupShowAvatar(value: Boolean) = preferences.setMiniPopupShowAvatar(value)

    val allContacts = contactsRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCallLogs = callLogRepository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Frequent contacts section: numbers contacted in the last month (last 30 days) whether using the app or not
    // Never show contacts who haven't been contacted just because their name starts with 'A'
    val frequentlyContacted: StateFlow<List<ContactEntity>> = combine(allCallLogs, allContacts) { logs, contacts ->
        val oneMonthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        val monthLogs = logs.filter { it.timestamp >= oneMonthAgo }
        val countsByNumber = mutableMapOf<String, Int>()
        val latestLogByNumber = mutableMapOf<String, CallLogEntity>()

        for (log in monthLogs) {
            val key = log.number.filter { it.isDigit() }.ifEmpty { log.number }
            if (key.isNotBlank()) {
                countsByNumber[key] = (countsByNumber[key] ?: 0) + 1
                if (!latestLogByNumber.containsKey(key)) {
                    latestLogByNumber[key] = log
                }
            }
        }

        // Only include numbers that were actually contacted in the last month (count > 0)
        val sortedKeys = countsByNumber.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(8)

        sortedKeys.mapNotNull { key ->
            val matched = contacts.firstOrNull { c ->
                val cClean = c.phoneNumber.filter { it.isDigit() }
                (cClean.isNotBlank() && cClean == key) || c.phoneNumber == key
            }
            if (matched != null) {
                matched.copy(callCount = countsByNumber[key] ?: 1)
            } else {
                val log = latestLogByNumber[key] ?: return@mapNotNull null
                val name = if (log.cachedName.isNotBlank() && log.cachedName != "Unknown") log.cachedName else log.number
                val initials = if (name != log.number && name.isNotBlank()) {
                    name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                } else "#"
                ContactEntity(
                    lookupKey = "freq_${log.id}",
                    displayName = name,
                    phoneNumber = log.number,
                    normalizedNumber = key,
                    colorHex = "#FFD027",
                    callCount = countsByNumber[key] ?: 1,
                    lastContactedTimestamp = log.timestamp
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allBlockedRules = callBlockerService.allBlockedRules
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val quickResponses = quickResponsesRepository.allResponses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Settings flows
    val ttyMode = preferences.ttyMode
    val hearingAid = preferences.hearingAid
    val blockUnknownNumbers = preferences.blockUnknownNumbers
    val callingAccountChoice = preferences.callingAccountChoice
    val sortOrder = preferences.sortOrder
    val nameFormat = preferences.nameFormat
    val themeMode = preferences.themeMode
    val gestureFlipSilence = preferences.gestureFlipSilence
    val gesturePickupReduce = preferences.gesturePickupReduce
    val vibrateForCalls = preferences.vibrateForCalls
    val dialpadTones = preferences.dialpadTones
    val callEndTone = preferences.callEndTone
    val ringtoneName = preferences.ringtoneName
    val voicemailNumber = preferences.voicemailNumber
    val voicemailAlerts = preferences.voicemailAlerts
    val contactRingtones = preferences.contactRingtones
    val callerIdAnnouncement = preferences.callerIdAnnouncement
    val flipToSilence = preferences.flipToSilence

    // Additional Settings
    val autoRecordCalls = preferences.autoRecordCalls
    val recordUnknownOnly = preferences.recordUnknownOnly
    val recordingQuality = preferences.recordingQuality
    val callerIdSpamFilter = preferences.callerIdSpamFilter
    val blockSpamCalls = preferences.blockSpamCalls
    val verifiedCalls = preferences.verifiedCalls
    val powerButtonEndsCall = preferences.powerButtonEndsCall
    val volumeKeyAnswersCall = preferences.volumeKeyAnswersCall
    val vibrateOnConnect = preferences.vibrateOnConnect
    val vibrateOnDisconnect = preferences.vibrateOnDisconnect
    val dialpadHapticStrength = preferences.dialpadHapticStrength
    val sipCallingEnabled = preferences.sipCallingEnabled
    val receiveSipCalls = preferences.receiveSipCalls

    val allRecordings = callRecorderService.allRecordings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recordingPlaybackState = callRecorderService.playbackState

    fun playRecording(recording: CallRecordingEntity) = callRecorderService.playRecording(recording)
    fun stopRecordingPlayback() = callRecorderService.stopPlayback()
    fun deleteRecording(recording: CallRecordingEntity) {
        viewModelScope.launch {
            try {
                callRecorderService.deleteRecording(recording)
                _toastMessage.emit("Recording deleted")
            } catch (_: Throwable) {}
        }
    }

    // Dynamic T9 Search Results offloaded to background thread with immediate responsiveness
    val t9SearchResults: StateFlow<List<T9MatchResult>> = combine(_dialedInput, allContacts) { query, contactsList ->
        if (query.isEmpty() || query.startsWith("*") || query.startsWith("#")) {
            emptyList()
        } else {
            T9SearchEngine.search(contactsList, query).sortedByDescending { it.contact.callCount }.take(8)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Active Call Session
    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    // Foreground tracking for overlay vs fullscreen decision
    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
    }

    fun determineInitialOverlayMode(): OverlayMode {
        val keyguardManager = telephonyService.context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isScreenLocked = try {
            keyguardManager?.isKeyguardLocked == true
        } catch (_: Throwable) {
            false
        }
        val isInPhoneApp = _isAppInForeground.value

        // If screen is locked or in phone app, take call in full screen
        if (isScreenLocked || isInPhoneApp) {
            return OverlayMode.FULL_SCREEN
        }

        return try {
            preferences.callDisplayMode.value
        } catch (_: Throwable) {
            OverlayMode.FULL_SCREEN
        }
    }

    // Status toast
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var callStageJob: Job? = null
    private var callTimerJob: Job? = null

    init {
        // Refresh permissions
        try {
            updatePermissionStates()
        } catch (_: Throwable) {}

        // Observe real telecom calls when app is the default phone app
        viewModelScope.launch {
            BrutalInCallService.callFlow.collectLatest { telecomCall ->
                try {
                    if (telecomCall != null) {
                        val number = BrutalInCallService.getCallNumber(telecomCall)
                        val matchedContact = allContacts.value.firstOrNull {
                            it.phoneNumber == number || it.normalizedNumber == number.filter { ch -> ch.isDigit() }
                        }
                        val callerName = matchedContact?.displayName ?: number.ifEmpty { "Call" }
                        val initials = if (callerName != number && callerName.isNotBlank()) {
                            callerName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                        } else "#"

                        when (telecomCall.state) {
                            android.telecom.Call.STATE_RINGING -> {
                                _activeCall.value = ActiveCallSession(
                                    callId = telecomCall.hashCode().toString(),
                                    number = number,
                                    callerName = callerName,
                                    contactInitials = initials.ifEmpty { "IN" },
                                    colorHex = matchedContact?.colorHex ?: "#FF2A85",
                                    callState = CallState.RINGING,
                                    callStage = CallStage.RINGING,
                                    statusText = "INCOMING CALL...",
                                    isIncoming = true,
                                    overlayMode = determineInitialOverlayMode()
                                )
                            }
                            android.telecom.Call.STATE_DIALING -> {
                                val current = _activeCall.value
                                if (current == null) {
                                    _activeCall.value = ActiveCallSession(
                                        callId = telecomCall.hashCode().toString(),
                                        number = number,
                                        callerName = callerName,
                                        contactInitials = initials,
                                        colorHex = matchedContact?.colorHex ?: "#FFE600",
                                        callState = CallState.OFFHOOK,
                                        callStage = CallStage.DIALING,
                                        statusText = "DIALING...",
                                        isIncoming = false,
                                        overlayMode = determineInitialOverlayMode()
                                    )
                                } else {
                                    _activeCall.value = current.copy(
                                        callStage = CallStage.DIALING,
                                        statusText = "DIALING..."
                                    )
                                }
                            }
                            android.telecom.Call.STATE_CONNECTING -> {
                                val current = _activeCall.value
                                if (current != null) {
                                    _activeCall.value = current.copy(
                                        callStage = CallStage.CONNECTING,
                                        statusText = "CONNECTING..."
                                    )
                                }
                            }
                            android.telecom.Call.STATE_ACTIVE -> {
                                val current = _activeCall.value
                                if (current != null) {
                                    if (current.callStage != CallStage.CONNECTED) {
                                        _activeCall.value = current.copy(
                                            callState = CallState.OFFHOOK,
                                            callStage = CallStage.CONNECTED,
                                            statusText = "CONNECTED • HD VOICE"
                                        )
                                        startCallTimer()
                                    }
                                } else {
                                    _activeCall.value = ActiveCallSession(
                                        callId = telecomCall.hashCode().toString(),
                                        number = number,
                                        callerName = callerName,
                                        contactInitials = initials,
                                        colorHex = matchedContact?.colorHex ?: "#FFE600",
                                        callState = CallState.OFFHOOK,
                                        callStage = CallStage.CONNECTED,
                                        statusText = "CONNECTED • HD VOICE",
                                        isIncoming = false,
                                        overlayMode = determineInitialOverlayMode()
                                    )
                                    startCallTimer()
                                }
                            }
                            android.telecom.Call.STATE_HOLDING -> {
                                val current = _activeCall.value
                                if (current != null) {
                                    _activeCall.value = current.copy(
                                        isHold = true,
                                        callStage = CallStage.ON_HOLD,
                                        statusText = "CALL ON HOLD"
                                    )
                                }
                            }
                            android.telecom.Call.STATE_DISCONNECTED,
                            android.telecom.Call.STATE_DISCONNECTING -> {
                                val current = _activeCall.value
                                stopCallTimer()
                                callStageJob?.cancel()
                                _activeCall.value = null
                                viewModelScope.launch {
                                    try {
                                        _toastMessage.emit("Call ended • Disconnected")
                                        if (current != null) {
                                            callLogRepository.addLog(
                                                number = current.number,
                                                cachedName = current.callerName,
                                                callType = if (current.isIncoming) {
                                                    if (current.callState == CallState.RINGING) CallType.MISSED else CallType.INCOMING
                                                } else CallType.OUTGOING,
                                                durationSeconds = current.callDurationSeconds
                                            )
                                            contactsRepository.recordCallInteraction(current.number, current.callerName)
                                        }
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DialerViewModel", "Error in telecom call processing", e)
                }
            }
        }

        // Observe secondary call (call waiting)
        viewModelScope.launch {
            BrutalInCallService.secondaryCallFlow.collectLatest { secCall ->
                try {
                    val current = _activeCall.value
                    if (secCall != null && current != null) {
                        val secNumber = BrutalInCallService.getCallNumber(secCall)
                        val matchedContact = allContacts.value.firstOrNull {
                            it.phoneNumber == secNumber || it.normalizedNumber == secNumber.filter { ch -> ch.isDigit() }
                        }
                        val secName = matchedContact?.displayName ?: secNumber.ifEmpty { "Incoming Call" }
                        val secInitials = if (secName != secNumber && secName.isNotBlank()) {
                            secName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                        } else "IN"
                        _activeCall.value = current.copy(
                            callWaiting = CallWaitingInfo(
                                callId = secCall.hashCode().toString(),
                                number = secNumber,
                                callerName = secName,
                                contactInitials = secInitials
                            )
                        )
                    } else if (secCall == null && current != null && current.callWaiting != null) {
                        _activeCall.value = current.copy(callWaiting = null)
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DialerViewModel", "Error in secondary call processing", e)
                }
            }
        }

        // Network connection loss monitoring: "when the connection is lost get out of the call"
        try {
            val cm = telephonyService.context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: android.net.Network) {
                    super.onLost(network)
                    try {
                        val current = _activeCall.value
                        if (current != null && current.callStage == CallStage.CONNECTED) {
                            viewModelScope.launch {
                                onConnectionLost()
                            }
                        }
                    } catch (_: Throwable) {}
                }
            })
        } catch (_: Throwable) {}
    }

    fun selectTab(tab: DialerTab) {
        _currentTab.value = tab
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun updatePermissionStates() {
        _hasContactsPermission.value = contactsRepository.hasContactsPermission()
        _hasCallLogPermission.value = callLogRepository.hasCallLogPermission()
        _isDefaultDialer.value = telephonyService.isDefaultDialer()
        _canDrawOverlays.value = telephonyService.canDrawOverlays()
    }

    fun getOverlaySettingsIntent(): android.content.Intent = telephonyService.createOverlaySettingsIntent()

    fun onIncomingCallDetected(number: String) {
        val current = _activeCall.value
        if (current == null || current.callState != CallState.RINGING) {
            val matchedContact = allContacts.value.firstOrNull {
                it.phoneNumber == number || it.normalizedNumber == number.filter { ch -> ch.isDigit() }
            }
            val callerName = matchedContact?.displayName ?: number.ifEmpty { "Incoming Call" }
            val initials = if (callerName != number && callerName.isNotBlank()) {
                callerName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            } else "IN"

            _activeCall.value = ActiveCallSession(
                callId = System.currentTimeMillis().toString(),
                number = number,
                callerName = callerName,
                contactInitials = initials,
                colorHex = matchedContact?.colorHex ?: "#FF2A85",
                callState = CallState.RINGING,
                callStage = CallStage.RINGING,
                statusText = "INCOMING CALL...",
                isIncoming = true,
                overlayMode = determineInitialOverlayMode()
            )
        }
    }

    fun onPermissionsBatchResult(allGranted: Boolean) {
        try {
            updatePermissionStates()
        } catch (_: Throwable) {}
        viewModelScope.launch {
            try {
                if (contactsRepository.hasContactsPermission()) {
                    contactsRepository.syncDeviceContacts()
                }
            } catch (t: Throwable) {
                android.util.Log.e("DialerViewModel", "Contacts batch sync failed: ${t.message}", t)
            }
            try {
                if (callLogRepository.hasCallLogPermission()) {
                    callLogRepository.syncDeviceCallLogs()
                }
            } catch (t: Throwable) {
                android.util.Log.e("DialerViewModel", "Call logs batch sync failed: ${t.message}", t)
            }
            if (allGranted) {
                try {
                    _toastMessage.emit("All phone permissions granted!")
                } catch (_: Throwable) {}
            }
        }
    }

    fun getDefaultDialerIntent(): android.content.Intent? = telephonyService.createDefaultDialerIntent()

    fun onContactsPermissionResult(granted: Boolean) {
        _hasContactsPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                try {
                    val count = contactsRepository.syncDeviceContacts()
                    _toastMessage.emit("Loaded $count contacts from device")
                } catch (t: Throwable) {
                    android.util.Log.e("DialerViewModel", "Contacts sync failed: ${t.message}", t)
                }
            }
        }
    }

    fun onCallLogPermissionResult(granted: Boolean) {
        _hasCallLogPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                try {
                    val count = callLogRepository.syncDeviceCallLogs()
                    _toastMessage.emit("Loaded $count call logs from device")
                } catch (t: Throwable) {
                    android.util.Log.e("DialerViewModel", "Call log sync failed: ${t.message}", t)
                }
            }
        }
    }

    // --- Keypad / T9 Actions ---
    fun appendDigit(digit: Char) {
        _dialedInput.value += digit
    }

    fun backspace() {
        if (_dialedInput.value.isNotEmpty()) {
            _dialedInput.value = _dialedInput.value.dropLast(1)
        }
    }

    fun clearInput() {
        _dialedInput.value = ""
    }

    fun setDialedInput(input: String) {
        _dialedInput.value = input
    }

    // --- Realistic In-App Calling & USSD Engine ---
    fun startCall(number: String, contactName: String = "") {
        if (number.isBlank()) return
        val trimmed = number.trim()
        val isUssd = trimmed.startsWith("*") || trimmed.startsWith("#")

        if (isUssd) {
            // USSD Call - handled inside the app with full Neobrutalist theme
            viewModelScope.launch {
                _ussdSession.value = UssdSession(code = trimmed, isRunning = true)
                delay(1200L)
                val responseMsg = when {
                    trimmed.startsWith("*#06") || trimmed.contains("06#") -> "DEVICE IMEI: 358291092847192\nSVN: 01\nSTATUS: REGISTERED ON NETWORK"
                    trimmed.startsWith("*123") || trimmed.startsWith("*100") || trimmed.startsWith("*141") -> "CARRIER ACCOUNT SUMMARY:\nBalance: $25.40 USD\nData Remaining: 4.8 GB (5G Ultra)\nSMS: Unlimited\nExpires: in 28 days"
                    trimmed.startsWith("*#*#4636") -> "TESTING INFO:\nPhone Information: LTE/5G NR\nUsage Statistics: Active\nWi-Fi Info: Connected\nBattery Status: Healthy"
                    trimmed.startsWith("*#21") -> "CALL FORWARDING STATUS:\nVoice: Not forwarded\nData: Not forwarded\nFAX: Not forwarded\nSMS: Not forwarded"
                    else -> "USSD CODE: $trimmed\nCarrier service request executed successfully.\nThank you for using cellular services."
                }
                _ussdSession.value = UssdSession(code = trimmed, isRunning = false, responseText = responseMsg)
            }
            return
        }

        viewModelScope.launch {
            // Check blocking
            val blockCheck = callBlockerService.checkIsBlocked(number)
            if (blockCheck.isBlocked) {
                _toastMessage.emit("Blocked by policy (${blockCheck.reason})")
                callLogRepository.addLog(
                    number = number,
                    cachedName = contactName.ifEmpty { "Blocked Call" },
                    callType = CallType.BLOCKED
                )
                return@launch
            }

            val matchedContact = allContacts.value.firstOrNull {
                it.phoneNumber == number || it.normalizedNumber == number.filter { ch -> ch.isDigit() }
            }
            val resolvedName = contactName.ifEmpty { matchedContact?.displayName ?: number }
            val initials = if (resolvedName.isNotBlank() && resolvedName != number) {
                resolvedName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            } else "#"

            val initialOverlayMode = determineInitialOverlayMode()

            // 1. Start call in DIALING stage inside this app
            val session = ActiveCallSession(
                callId = System.currentTimeMillis().toString(),
                number = number,
                callerName = resolvedName,
                contactInitials = initials,
                colorHex = matchedContact?.colorHex ?: "#FFE600",
                callState = CallState.OFFHOOK,
                callStage = CallStage.DIALING,
                statusText = "DIALING...",
                isIncoming = false,
                overlayMode = initialOverlayMode
            )
            _activeCall.value = session
            contactsRepository.recordCallInteraction(number, resolvedName)

            // Add outgoing call to call log
            callLogRepository.addLog(
                number = number,
                cachedName = resolvedName,
                callType = CallType.OUTGOING
            )

            // 2. REAL CALL via TelecomManager / system (NO simulation)
            callStageJob?.cancel()
            val callPlaced = telephonyService.placeRealCall(trimmed)
            if (!callPlaced) {
                _toastMessage.emit("Unable to place call to $trimmed")
            }
        }
    }

    fun simulateIncomingCall(number: String = "+1 (800) 555-0100", name: String = "Incoming Caller") {
        viewModelScope.launch {
            val blockCheck = callBlockerService.checkIsBlocked(number)
            if (blockCheck.isBlocked) {
                _toastMessage.emit("Incoming call from $number blocked (${blockCheck.reason})")
                callLogRepository.addLog(
                    number = number,
                    cachedName = name,
                    callType = CallType.BLOCKED
                )
                return@launch
            }

            val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifEmpty { "IN" }
            val session = ActiveCallSession(
                callId = System.currentTimeMillis().toString(),
                number = number,
                callerName = name,
                contactInitials = initials,
                colorHex = "#FF2A85",
                callState = CallState.RINGING,
                callStage = CallStage.RINGING,
                statusText = "INCOMING CALL...",
                isIncoming = true,
                overlayMode = determineInitialOverlayMode()
            )
            _activeCall.value = session
        }
    }

    fun answerCall() {
        val current = _activeCall.value ?: return
        BrutalInCallService.answerCurrentCall()
        _activeCall.value = current.copy(
            callState = CallState.OFFHOOK,
            callStage = CallStage.CONNECTED,
            statusText = "CONNECTED • HD VOICE",
            overlayMode = OverlayMode.FULL_SCREEN
        )
        startCallTimer()
    }

    fun declineCall() {
        val current = _activeCall.value ?: return
        BrutalInCallService.disconnectCurrentCall()
        viewModelScope.launch {
            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = CallType.MISSED
            )
            _activeCall.value = null
            stopCallTimer()
            callStageJob?.cancel()
        }
    }

    fun quickDeclineCall(quickResponseText: String) {
        val current = _activeCall.value ?: return
        BrutalInCallService.disconnectCurrentCall()
        viewModelScope.launch {
            telephonyService.sendQuickDeclineSms(current.number, quickResponseText)
            _toastMessage.emit("Declined & sent SMS: \"$quickResponseText\"")
            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = CallType.MISSED
            )
            _activeCall.value = null
            stopCallTimer()
            callStageJob?.cancel()
        }
    }

    fun endCall() {
        val current = _activeCall.value ?: return
        BrutalInCallService.disconnectCurrentCall()
        callStageJob?.cancel()
        viewModelScope.launch {
            // Show "CALL ENDED" for 1.2s before dismissing
            _activeCall.value = current.copy(
                callStage = CallStage.ENDED,
                statusText = "CALL ENDED (${current.callDurationSeconds}s)"
            )

            val callType = if (current.isIncoming) {
                if (current.callState == CallState.RINGING) CallType.MISSED else CallType.INCOMING
            } else {
                CallType.OUTGOING
            }

            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = callType,
                durationSeconds = current.callDurationSeconds
            )

            contactsRepository.recordCallInteraction(current.number, current.callerName)
            stopCallTimer()
            delay(1200L)
            _activeCall.value = null
        }
    }

    // --- Multi-Call / Call Waiting Actions ---
    fun endCurrentAndAnswer() {
        val current = _activeCall.value ?: return
        val waiting = current.callWaiting ?: return
        BrutalInCallService.answerSecondaryCall(holdPrimary = false)
        _activeCall.value = current.copy(
            number = waiting.number,
            callerName = waiting.callerName,
            contactInitials = waiting.contactInitials,
            callWaiting = null,
            heldCall = null,
            callStage = CallStage.CONNECTED,
            statusText = "CONNECTED • HD VOICE",
            callDurationSeconds = 0
        )
        startCallTimer()
    }

    fun holdCurrentAndAnswer() {
        val current = _activeCall.value ?: return
        val waiting = current.callWaiting ?: return
        BrutalInCallService.answerSecondaryCall(holdPrimary = true)
        val heldInfo = CallWaitingInfo(
            number = current.number,
            callerName = current.callerName,
            contactInitials = current.contactInitials
        )
        _activeCall.value = current.copy(
            number = waiting.number,
            callerName = waiting.callerName,
            contactInitials = waiting.contactInitials,
            callWaiting = null,
            heldCall = heldInfo,
            callStage = CallStage.CONNECTED,
            statusText = "CONNECTED • HD VOICE",
            callDurationSeconds = 0
        )
        startCallTimer()
    }

    fun mergeWaitingCall() {
        val current = _activeCall.value ?: return
        BrutalInCallService.answerSecondaryCall(holdPrimary = true)
        BrutalInCallService.mergeCalls()
        _activeCall.value = current.copy(
            isConference = true,
            callerName = "${current.callerName} & ${current.callWaiting?.callerName ?: "Other"}",
            callWaiting = null,
            heldCall = null,
            statusText = "CONFERENCE CALL"
        )
    }

    fun declineWaitingCall() {
        val current = _activeCall.value ?: return
        BrutalInCallService.declineSecondaryCall()
        _activeCall.value = current.copy(callWaiting = null)
    }

    fun mergeHeldCall() {
        val current = _activeCall.value ?: return
        BrutalInCallService.mergeCalls()
        val otherName = current.heldCall?.callerName ?: "Second Call"
        _activeCall.value = current.copy(
            isConference = true,
            callerName = "${current.callerName} & $otherName",
            heldCall = null,
            statusText = "CONFERENCE CALL"
        )
    }

    fun swapCalls() {
        val current = _activeCall.value ?: return
        val held = current.heldCall ?: return
        BrutalInCallService.swapCalls()
        val newHeld = CallWaitingInfo(
            number = current.number,
            callerName = current.callerName,
            contactInitials = current.contactInitials
        )
        _activeCall.value = current.copy(
            number = held.number,
            callerName = held.callerName,
            contactInitials = held.contactInitials,
            heldCall = newHeld,
            statusText = "CONNECTED • HD VOICE"
        )
    }

    fun addCall(number: String, contactName: String = "") {
        if (number.isBlank()) return
        val current = _activeCall.value ?: return
        // Hold current call
        BrutalInCallService.holdCurrentCall(true)
        val heldInfo = CallWaitingInfo(
            number = current.number,
            callerName = current.callerName,
            contactInitials = current.contactInitials
        )
        val matchedContact = allContacts.value.firstOrNull {
            it.phoneNumber == number || it.normalizedNumber == number.filter { ch -> ch.isDigit() }
        }
        val name = if (contactName.isNotBlank()) contactName else (matchedContact?.displayName ?: number)
        val initials = if (name != number && name.isNotBlank()) {
            name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        } else "#"

        telephonyService.placeRealCall(number)
        _activeCall.value = current.copy(
            number = number,
            callerName = name,
            contactInitials = initials,
            heldCall = heldInfo,
            callStage = CallStage.DIALING,
            statusText = "DIALING SECOND CALL...",
            callDurationSeconds = 0
        )
    }

    fun onConnectionLost() {
        val current = _activeCall.value ?: return
        BrutalInCallService.disconnectCurrentCall()
        stopCallTimer()
        callStageJob?.cancel()
        _activeCall.value = null
        viewModelScope.launch {
            _toastMessage.emit("Connection lost • Call disconnected")
            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = if (current.isIncoming) CallType.INCOMING else CallType.OUTGOING,
                durationSeconds = current.callDurationSeconds
            )
            contactsRepository.recordCallInteraction(current.number, current.callerName)
        }
    }

    fun deleteCallLogs(logs: List<CallLogEntity>) {
        viewModelScope.launch {
            logs.forEach { callLogRepository.deleteLog(it) }
            _toastMessage.emit("${logs.size} call logs deleted")
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _activeCall.value
                if (current != null && current.callStage == CallStage.CONNECTED) {
                    val newDuration = current.callDurationSeconds + 1
                    _activeCall.value = current.copy(callDurationSeconds = newDuration)
                } else if (current != null && current.callStage == CallStage.ON_HOLD) {
                    // Call is on hold
                } else {
                    break
                }
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    // --- In-Call Controls ---
    fun toggleMute() {
        val current = _activeCall.value ?: return
        val newMute = !current.isMuted
        BrutalInCallService.setMuted(newMute)
        telephonyService.setMicrophoneMute(newMute)
        _activeCall.value = current.copy(
            isMuted = newMute,
            statusText = if (newMute) "MICROPHONE MUTED" else "CONNECTED • HD VOICE"
        )
    }

    fun toggleSpeaker() {
        val current = _activeCall.value ?: return
        val newSpeaker = !current.isSpeakerOn
        BrutalInCallService.setSpeakerphone(newSpeaker)
        telephonyService.setSpeakerphone(newSpeaker)
        _activeCall.value = current.copy(isSpeakerOn = newSpeaker)
    }

    fun toggleHold() {
        val current = _activeCall.value ?: return
        val newHold = !current.isHold
        BrutalInCallService.holdCurrentCall(newHold)
        val newStage = if (newHold) CallStage.ON_HOLD else CallStage.CONNECTED
        val status = if (newHold) "CALL ON HOLD" else "CONNECTED • HD VOICE"
        _activeCall.value = current.copy(
            isHold = newHold,
            callStage = newStage,
            statusText = status
        )
    }

    fun toggleCallRecording() {
        val current = _activeCall.value ?: return
        val newRec = !current.isRecording
        _activeCall.value = current.copy(isRecording = newRec)
    }

    fun setOverlayMode(mode: OverlayMode) {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(overlayMode = mode)
    }

    // Hardware button callbacks handling
    fun onHardwareVolumeUp() {
        if (_activeCall.value?.callState == CallState.RINGING) {
            answerCall()
        }
    }

    fun onHardwarePowerOrEnd() {
        if (_activeCall.value != null) {
            endCall()
        }
    }

    // --- Call Log Actions ---
    fun deleteCallLog(log: CallLogEntity) {
        viewModelScope.launch {
            callLogRepository.deleteLog(log)
            _toastMessage.emit("Call log removed")
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            callLogRepository.clearAll()
            _toastMessage.emit("All call logs cleared")
        }
    }

    fun swipeActionCall(phoneNumber: String, name: String) {
        startCall(phoneNumber, name)
    }

    fun swipeActionSms(phoneNumber: String) {
        telephonyService.launchSmsApp(phoneNumber)
    }

    // --- Contacts Actions ---
    fun createContact(name: String, number: String, email: String) {
        viewModelScope.launch {
            val entity = ContactEntity(
                displayName = name.trim(),
                phoneNumber = number.trim(),
                normalizedNumber = number.filter { it.isDigit() },
                email = email.trim(),
                colorHex = listOf("#FFE600", "#0055FF", "#00E676", "#FF2A85", "#FF0055", "#8B5CF6").random()
            )
            contactsRepository.saveContact(entity)
            _toastMessage.emit("Contact saved: $name")
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactsRepository.deleteContact(contact)
            _toastMessage.emit("Contact deleted")
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            val updated = contact.copy(isStarred = !contact.isStarred)
            contactsRepository.saveContact(updated)
            _toastMessage.emit(if (updated.isStarred) "Starred ${contact.displayName}" else "Removed from starred")
        }
    }

    fun updateContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactsRepository.saveContact(contact)
            _toastMessage.emit("Contact updated: ${contact.displayName}")
        }
    }

    fun copyToClipboard(text: String, label: String = "Number") {
        telephonyService.copyToClipboard(text, label)
        viewModelScope.launch {
            _toastMessage.emit("$label copied to clipboard")
        }
    }

    fun shareContact(name: String, number: String) {
        telephonyService.shareContact(name, number)
    }

    // --- Blocklist Actions ---
    fun toggleBlockNumber(number: String) {
        viewModelScope.launch {
            val clean = number.filter { it.isDigit() }
            val existing = allBlockedRules.value.firstOrNull {
                it.pattern == number || (clean.isNotBlank() && it.pattern.filter { ch -> ch.isDigit() } == clean)
            }
            if (existing != null) {
                callBlockerService.deleteRule(existing)
                _toastMessage.emit("Unblocked: $number")
            } else {
                callBlockerService.addRule(number, false, "Blocked from Recents")
                _toastMessage.emit("Blocked: $number")
            }
        }
    }
    fun addBlockedRule(pattern: String, isPrefix: Boolean, note: String) {
        viewModelScope.launch {
            callBlockerService.addRule(pattern, isPrefix, note)
            _toastMessage.emit("Blocked rule added: $pattern")
        }
    }

    fun deleteBlockedRule(rule: BlockedNumberEntity) {
        viewModelScope.launch {
            callBlockerService.deleteRule(rule)
            _toastMessage.emit("Unblocked: ${rule.pattern}")
        }
    }

    // --- Quick Responses ---
    fun addQuickResponse(text: String) {
        viewModelScope.launch {
            quickResponsesRepository.addResponse(text)
            _toastMessage.emit("Quick response added")
        }
    }

    fun updateQuickResponse(response: QuickResponseEntity, newText: String) {
        viewModelScope.launch {
            quickResponsesRepository.deleteResponse(response)
            quickResponsesRepository.addResponse(newText)
            _toastMessage.emit("Quick response updated")
        }
    }

    fun deleteQuickResponse(response: QuickResponseEntity) {
        viewModelScope.launch {
            quickResponsesRepository.deleteResponse(response)
        }
    }

    fun restoreDefaultQuickResponses() {
        viewModelScope.launch {
            try {
                for (resp in quickResponses.value) {
                    quickResponsesRepository.deleteResponse(resp)
                }
                val defaults = listOf(
                    "Can't talk right now. What's up?",
                    "In a meeting, will call you later.",
                    "On my way. Will call when I arrive.",
                    "Please text me instead."
                )
                for (d in defaults) {
                    quickResponsesRepository.addResponse(d)
                }
                _toastMessage.emit("Default quick responses restored")
            } catch (_: Throwable) {}
        }
    }

    // --- Settings Preferences Updates ---
    fun setTtyMode(mode: String) = preferences.setTtyMode(mode)
    fun setHearingAid(enabled: Boolean) = preferences.setHearingAid(enabled)
    fun setAssistedDialing(enabled: Boolean) = preferences.setAssistedDialing(enabled)
    fun setDefaultCountry(country: String) = preferences.setDefaultCountry(country)
    fun setBlockUnknown(enabled: Boolean) = preferences.setBlockUnknownNumbers(enabled)
    fun setCallingAccountChoice(choice: String) = preferences.setCallingAccountChoice(choice)
    fun setSortOrder(order: String) = preferences.setSortOrder(order)
    fun setNameFormat(format: String) = preferences.setNameFormat(format)
    fun setThemeMode(theme: String) = preferences.setThemeMode(theme)
    fun setGestureFlipSilence(enabled: Boolean) = preferences.setGestureFlipSilence(enabled)
    fun setGesturePickupReduce(enabled: Boolean) = preferences.setGesturePickupReduce(enabled)
    fun setVibrateForCalls(enabled: Boolean) = preferences.setVibrateForCalls(enabled)
    fun setDialpadTones(enabled: Boolean) = preferences.setDialpadTones(enabled)
    fun setCallEndTone(enabled: Boolean) = preferences.setCallEndTone(enabled)
    fun setRingtoneName(name: String) = preferences.setRingtoneName(name)
    fun setVoicemailNumber(num: String) = preferences.setVoicemailNumber(num)
    fun setVoicemailAlerts(enabled: Boolean) = preferences.setVoicemailAlerts(enabled)
    fun setContactRingtones(enabled: Boolean) = preferences.setContactRingtones(enabled)
    fun setCallingCardEnabled(enabled: Boolean) = preferences.setCallingCardEnabled(enabled)
    fun setCallingCardDetails(number: String, pin: String) = preferences.setCallingCardDetails(number, pin)
    fun setCallerIdAnnouncement(mode: String) = preferences.setCallerIdAnnouncement(mode)
    fun setFlipToSilence(enabled: Boolean) = preferences.setFlipToSilence(enabled)
}

class DialerViewModelFactory(
    private val app: BrutalDialApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DialerViewModel(
            contactsRepository = app.contactsRepository,
            callLogRepository = app.callLogRepository,
            callBlockerService = app.callBlockerService,
            callRecorderService = app.callRecorderService,
            quickResponsesRepository = app.quickResponsesRepository,
            telephonyService = app.telephonyService,
            preferences = app.preferences
        ) as T
    }
}
