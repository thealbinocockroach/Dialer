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
    SPEED_DIAL,
    RECENTS,
    KEYPAD,
    CONTACTS,
    RECORDINGS,
    SETTINGS
}

class DialerViewModel(
    private val contactsRepository: ContactsRepository,
    private val callLogRepository: CallLogRepository,
    private val callBlockerService: CallBlockerService,
    private val callRecorderService: CallRecorderService,
    private val quickResponsesRepository: QuickResponsesRepository,
    private val telephonyService: TelephonyService,
    private val preferences: SettingsPreferences
) : ViewModel() {

    private val _currentTab = MutableStateFlow(DialerTab.KEYPAD)
    val currentTab: StateFlow<DialerTab> = _currentTab.asStateFlow()

    private val _dialedInput = MutableStateFlow("")
    val dialedInput: StateFlow<String> = _dialedInput.asStateFlow()

    val allContacts = contactsRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frequentlyContacted = contactsRepository.frequentlyContacted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speedDialContacts = contactsRepository.speedDialContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCallLogs = callLogRepository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordings = callRecorderService.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBlockedRules = callBlockerService.allBlockedRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickResponses = quickResponsesRepository.allResponses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState = callRecorderService.playbackState

    val autoAnswerEnabled = preferences.autoAnswerEnabled
    val autoAnswerDelay = preferences.autoAnswerDelay
    val autoRecordRule = preferences.autoRecordRule
    val blockUnknownNumbers = preferences.blockUnknownNumbers

    // Dynamic T9 Search Results
    val t9SearchResults: StateFlow<List<T9MatchResult>> = combine(_dialedInput, allContacts) { query, contactsList ->
        if (query.isEmpty()) {
            emptyList()
        } else {
            T9SearchEngine.search(contactsList, query).take(10)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Call Session State Machine
    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    // Status snackbar / notice
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var callTimerJob: Job? = null
    private var autoAnswerJob: Job? = null

    fun selectTab(tab: DialerTab) {
        _currentTab.value = tab
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

    fun onSpeedDialLongPress(digit: Int) {
        viewModelScope.launch {
            val contact = contactsRepository.getBySpeedDial(digit)
            if (contact != null) {
                _toastMessage.emit("Speed Dial $digit: Calling ${contact.displayName}")
                startCall(contact.phoneNumber, contact.displayName)
            } else {
                _toastMessage.emit("Speed Dial $digit is unassigned")
            }
        }
    }

    fun assignSpeedDial(contactId: Long, digit: Int) {
        viewModelScope.launch {
            contactsRepository.setSpeedDial(contactId, digit)
            _toastMessage.emit("Assigned to Speed Dial #$digit")
        }
    }

    fun clearSpeedDial(digit: Int) {
        viewModelScope.launch {
            contactsRepository.clearSpeedDial(digit)
            _toastMessage.emit("Cleared Speed Dial #$digit")
        }
    }

    // --- Outgoing Call Flow ---
    fun startCall(number: String, contactName: String = "") {
        if (number.isBlank()) return
        viewModelScope.launch {
            // Check blocking
            val blockCheck = callBlockerService.checkIsBlocked(number)
            if (blockCheck.isBlocked) {
                _toastMessage.emit("Number is blocked! Call cancelled.")
                callLogRepository.addLog(
                    number = number,
                    cachedName = contactName.ifEmpty { "Blocked Target" },
                    callType = CallType.BLOCKED
                )
                return@launch
            }

            val matchedContact = allContacts.value.firstOrNull {
                it.phoneNumber == number || it.normalizedNumber == number.filter { ch -> ch.isDigit() }
            }
            val resolvedName = contactName.ifEmpty { matchedContact?.displayName ?: "Unknown" }
            val initials = if (resolvedName.isNotBlank() && resolvedName != "Unknown") {
                resolvedName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            } else "#"

            val session = ActiveCallSession(
                callId = System.currentTimeMillis().toString(),
                number = number,
                callerName = resolvedName,
                contactInitials = initials,
                colorHex = matchedContact?.colorHex ?: "#FFE600",
                callState = CallState.OFFHOOK,
                isIncoming = false,
                overlayMode = OverlayMode.FULL_SCREEN
            )
            _activeCall.value = session
            contactsRepository.recordCallInteraction(number, resolvedName)
            startCallTimer()

            // Check auto record rule
            checkAndTriggerAutoRecord(number, matchedContact != null)

            // Launch system dialer in background for real telecom dispatch
            telephonyService.launchSystemDialer(number)
        }
    }

    // --- Incoming Call Flow (Simulation & Real) ---
    fun simulateIncomingCall(number: String = "+1 (555) 019-2831", name: String = "Alex Vance") {
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
                isIncoming = true,
                overlayMode = OverlayMode.POP_UP
            )
            _activeCall.value = session

            // Check Auto-Answer controller
            if (preferences.autoAnswerEnabled.value) {
                val delaySec = preferences.autoAnswerDelay.value
                val headsetOnly = preferences.autoAnswerHeadsetOnly.value
                val headsetConnected = telephonyService.isHeadsetOrBluetoothConnected()

                if (!headsetOnly || headsetConnected) {
                    autoAnswerJob?.cancel()
                    autoAnswerJob = viewModelScope.launch {
                        _toastMessage.emit("Auto-answering in ${delaySec}s...")
                        delay(delaySec * 1000L)
                        if (_activeCall.value?.callState == CallState.RINGING) {
                            answerCall()
                            _toastMessage.emit("Call auto-answered")
                        }
                    }
                }
            }
        }
    }

    fun answerCall() {
        autoAnswerJob?.cancel()
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(
            callState = CallState.OFFHOOK,
            overlayMode = OverlayMode.FULL_SCREEN
        )
        startCallTimer()

        // Auto record check
        val isSavedContact = allContacts.value.any { it.phoneNumber == current.number }
        checkAndTriggerAutoRecord(current.number, isSavedContact)
    }

    fun declineCall() {
        autoAnswerJob?.cancel()
        val current = _activeCall.value ?: return
        viewModelScope.launch {
            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = CallType.MISSED
            )
            _activeCall.value = null
            stopCallTimer()
        }
    }

    fun quickDeclineCall(quickResponseText: String) {
        autoAnswerJob?.cancel()
        val current = _activeCall.value ?: return
        viewModelScope.launch {
            val sent = telephonyService.sendQuickDeclineSms(current.number, quickResponseText)
            _toastMessage.emit(if (sent) "Declined & SMS sent" else "Declined with message draft")
            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = CallType.MISSED
            )
            _activeCall.value = null
            stopCallTimer()
        }
    }

    fun endCall() {
        autoAnswerJob?.cancel()
        val current = _activeCall.value ?: return
        viewModelScope.launch {
            var recordingId: Long? = null
            var recordingPath: String? = null
            var wasRecorded = false

            if (current.isRecording) {
                val savedRecording = callRecorderService.stopRecording(current.number, current.callerName)
                if (savedRecording != null) {
                    recordingId = savedRecording.id
                    recordingPath = savedRecording.filePath
                    wasRecorded = true
                    _toastMessage.emit("Call recording saved (${savedRecording.durationSeconds}s)")
                }
            }

            val callType = if (current.isIncoming) {
                if (current.callState == CallState.RINGING) CallType.MISSED else CallType.INCOMING
            } else {
                CallType.OUTGOING
            }

            callLogRepository.addLog(
                number = current.number,
                cachedName = current.callerName,
                callType = callType,
                durationSeconds = current.callDurationSeconds,
                isRecorded = wasRecorded,
                recordingId = recordingId,
                recordingPath = recordingPath
            )

            contactsRepository.recordCallInteraction(current.number, current.callerName)
            _activeCall.value = null
            stopCallTimer()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _activeCall.value
                if (current != null && current.callState == CallState.OFFHOOK) {
                    val newDuration = current.callDurationSeconds + 1
                    val newRecDuration = if (current.isRecording) current.recordingDurationSeconds + 1 else 0
                    _activeCall.value = current.copy(
                        callDurationSeconds = newDuration,
                        recordingDurationSeconds = newRecDuration
                    )
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
        _activeCall.value = current.copy(isMuted = newMute)
    }

    fun toggleSpeaker() {
        val current = _activeCall.value ?: return
        val newSpeaker = !current.isSpeakerOn
        telephonyService.setSpeakerphone(newSpeaker)
        _activeCall.value = current.copy(isSpeakerOn = newSpeaker)
    }

    fun toggleHold() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isHold = !current.isHold)
    }

    fun toggleCallRecording() {
        val current = _activeCall.value ?: return
        viewModelScope.launch {
            if (current.isRecording) {
                val saved = callRecorderService.stopRecording(current.number, current.callerName)
                _activeCall.value = current.copy(isRecording = false)
                _toastMessage.emit("Recording stopped")
            } else {
                val path = callRecorderService.startRecording(current.number, current.callerName)
                _activeCall.value = current.copy(
                    isRecording = true,
                    recordingFilePath = path
                )
                _toastMessage.emit("Recording active")
            }
        }
    }

    fun setOverlayMode(mode: OverlayMode) {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(overlayMode = mode)
    }

    private fun checkAndTriggerAutoRecord(phoneNumber: String, isSavedContact: Boolean) {
        val rule = preferences.autoRecordRule.value
        val shouldRecord = when (rule) {
            AutoRecordRule.ALL -> true
            AutoRecordRule.UNSAVED -> !isSavedContact
            AutoRecordRule.SPECIFIC_CONTACTS -> {
                allContacts.value.firstOrNull { it.phoneNumber == phoneNumber }?.isAutoRecord == true
            }
            AutoRecordRule.OFF -> false
        }

        if (shouldRecord) {
            viewModelScope.launch {
                val current = _activeCall.value ?: return@launch
                val path = callRecorderService.startRecording(current.number, current.callerName)
                _activeCall.value = current.copy(
                    isRecording = true,
                    recordingFilePath = path
                )
                _toastMessage.emit("Auto-recording started ($rule)")
            }
        }
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

    // --- Recordings Playback & Deletion ---
    fun playRecording(recording: CallRecordingEntity) {
        callRecorderService.playRecording(recording)
    }

    fun pauseRecording() {
        callRecorderService.pausePlayback()
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        viewModelScope.launch {
            callRecorderService.deleteRecording(recording)
            _toastMessage.emit("Recording deleted")
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
    fun createContact(name: String, number: String, email: String, speedDial: Int? = null) {
        viewModelScope.launch {
            val entity = ContactEntity(
                displayName = name.trim(),
                phoneNumber = number.trim(),
                normalizedNumber = number.filter { it.isDigit() },
                email = email.trim(),
                speedDialDigit = speedDial,
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

    fun deleteQuickResponse(response: QuickResponseEntity) {
        viewModelScope.launch {
            quickResponsesRepository.deleteResponse(response)
        }
    }

    // --- Settings Preferences Updates ---
    fun setAutoAnswer(enabled: Boolean) = preferences.setAutoAnswerEnabled(enabled)
    fun setAutoAnswerDelaySeconds(sec: Int) = preferences.setAutoAnswerDelay(sec)
    fun setAutoAnswerHeadsetOnly(headsetOnly: Boolean) = preferences.setAutoAnswerHeadsetOnly(headsetOnly)
    fun setAutoRecord(rule: AutoRecordRule) = preferences.setAutoRecordRule(rule)
    fun setBlockUnknown(enabled: Boolean) = preferences.setBlockUnknownNumbers(enabled)
    fun setDarkMode(dark: Boolean) = preferences.setDarkMode(dark)
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
