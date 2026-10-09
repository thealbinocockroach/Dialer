package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BrutalDialApplication
import com.example.data.local.SettingsPreferences
import com.example.data.model.*
import com.example.repository.*
import com.example.t9.T9MatchResult
import com.example.t9.T9SearchEngine
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

    private val _currentTab = MutableStateFlow(DialerTab.KEYPAD)
    val currentTab: StateFlow<DialerTab> = _currentTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _dialedInput = MutableStateFlow("")
    val dialedInput: StateFlow<String> = _dialedInput.asStateFlow()

    private val _hasCallLogPermission = MutableStateFlow(callLogRepository.hasCallLogPermission())
    val hasCallLogPermission: StateFlow<Boolean> = _hasCallLogPermission.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(contactsRepository.hasContactsPermission())
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frequentlyContacted = contactsRepository.frequentlyContacted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCallLogs = callLogRepository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBlockedRules = callBlockerService.allBlockedRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickResponses = quickResponsesRepository.allResponses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings flows
    val ttyMode = preferences.ttyMode
    val hearingAid = preferences.hearingAid
    val assistedDialing = preferences.assistedDialing
    val defaultCountry = preferences.defaultCountry
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
    val callingCardEnabled = preferences.callingCardEnabled
    val callingCardNumber = preferences.callingCardNumber
    val callingCardPin = preferences.callingCardPin
    val callerIdAnnouncement = preferences.callerIdAnnouncement
    val flipToSilence = preferences.flipToSilence

    // Dynamic T9 Search Results offloaded to background thread
    val t9SearchResults: StateFlow<List<T9MatchResult>> = combine(_dialedInput, allContacts) { query, contactsList ->
        if (query.isEmpty() || query.startsWith("*") || query.startsWith("#")) {
            emptyList()
        } else {
            T9SearchEngine.search(contactsList, query).take(8)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Call Session
    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    // Status toast
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var callStageJob: Job? = null
    private var callTimerJob: Job? = null

    init {
        // Refresh permissions
        updatePermissionStates()
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
    }

    fun onContactsPermissionResult(granted: Boolean) {
        _hasContactsPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                val count = contactsRepository.syncDeviceContacts()
                _toastMessage.emit("Loaded $count contacts from device")
            }
        }
    }

    fun onCallLogPermissionResult(granted: Boolean) {
        _hasCallLogPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                val count = callLogRepository.syncDeviceCallLogs()
                _toastMessage.emit("Loaded $count call logs from device")
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

            val initialOverlayMode = preferences.callDisplayMode.value

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

            // Progress realistically through call stages:
            callStageJob?.cancel()
            callStageJob = launch {
                // Stage 1: Dialing (1.8s)
                delay(1800L)
                if (_activeCall.value == null) return@launch
                _activeCall.value = _activeCall.value?.copy(
                    callStage = CallStage.CONNECTING,
                    statusText = "CONNECTING..."
                )

                // Stage 2: Connecting (1.5s)
                delay(1500L)
                if (_activeCall.value == null) return@launch
                _activeCall.value = _activeCall.value?.copy(
                    callStage = CallStage.RINGING,
                    statusText = "RINGING..."
                )

                // Stage 3: Ringing (2.2s)
                delay(2200L)
                if (_activeCall.value == null) return@launch
                _activeCall.value = _activeCall.value?.copy(
                    callStage = CallStage.CONNECTED,
                    statusText = "CONNECTED • HD VOICE"
                )

                // Stage 4: Connected - start duration timer
                startCallTimer()
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
                overlayMode = OverlayMode.POP_UP
            )
            _activeCall.value = session
        }
    }

    fun answerCall() {
        val current = _activeCall.value ?: return
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
        telephonyService.setMicrophoneMute(newMute)
        _activeCall.value = current.copy(
            isMuted = newMute,
            statusText = if (newMute) "MICROPHONE MUTED" else "CONNECTED • HD VOICE"
        )
    }

    fun toggleSpeaker() {
        val current = _activeCall.value ?: return
        val newSpeaker = !current.isSpeakerOn
        telephonyService.setSpeakerphone(newSpeaker)
        _activeCall.value = current.copy(isSpeakerOn = newSpeaker)
    }

    fun toggleHold() {
        val current = _activeCall.value ?: return
        val newHold = !current.isHold
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

    // --- Blocklist Actions ---
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
