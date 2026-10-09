package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.OverlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("brutal_dial_prefs", Context.MODE_PRIVATE)

    // Call display HUD mode (Full Screen, Pop-Up, Mini Pop-Up)
    private val _callDisplayMode = MutableStateFlow(
        OverlayMode.valueOf(prefs.getString("call_display_mode", OverlayMode.FULL_SCREEN.name) ?: OverlayMode.FULL_SCREEN.name)
    )
    val callDisplayMode: StateFlow<OverlayMode> = _callDisplayMode.asStateFlow()

    fun setCallDisplayMode(mode: OverlayMode) {
        prefs.edit().putString("call_display_mode", mode.name).apply()
        _callDisplayMode.value = mode
    }

    // Mini Pop-Up Specific Options
    private val _miniPopupKeepAfterAnswer = MutableStateFlow(prefs.getBoolean("mini_popup_keep_after_answer", true))
    val miniPopupKeepAfterAnswer: StateFlow<Boolean> = _miniPopupKeepAfterAnswer.asStateFlow()

    fun setMiniPopupKeepAfterAnswer(value: Boolean) {
        prefs.edit().putBoolean("mini_popup_keep_after_answer", value).apply()
        _miniPopupKeepAfterAnswer.value = value
    }

    private val _miniPopupAutoMinimize = MutableStateFlow(prefs.getBoolean("mini_popup_auto_minimize", false))
    val miniPopupAutoMinimize: StateFlow<Boolean> = _miniPopupAutoMinimize.asStateFlow()

    fun setMiniPopupAutoMinimize(value: Boolean) {
        prefs.edit().putBoolean("mini_popup_auto_minimize", value).apply()
        _miniPopupAutoMinimize.value = value
    }

    private val _miniPopupShowAvatar = MutableStateFlow(prefs.getBoolean("mini_popup_show_avatar", true))
    val miniPopupShowAvatar: StateFlow<Boolean> = _miniPopupShowAvatar.asStateFlow()

    fun setMiniPopupShowAvatar(value: Boolean) {
        prefs.edit().putBoolean("mini_popup_show_avatar", value).apply()
        _miniPopupShowAvatar.value = value
    }

    // 1. Accessibility
    private val _ttyMode = MutableStateFlow(prefs.getString("tty_mode", "OFF") ?: "OFF")
    val ttyMode: StateFlow<String> = _ttyMode.asStateFlow()

    private val _hearingAid = MutableStateFlow(prefs.getBoolean("hearing_aid", false))
    val hearingAid: StateFlow<Boolean> = _hearingAid.asStateFlow()

    // 2. Assisted Dialing
    private val _assistedDialing = MutableStateFlow(prefs.getBoolean("assisted_dialing", true))
    val assistedDialing: StateFlow<Boolean> = _assistedDialing.asStateFlow()

    private val _defaultCountry = MutableStateFlow(prefs.getString("default_country", "United States (+1)") ?: "United States (+1)")
    val defaultCountry: StateFlow<String> = _defaultCountry.asStateFlow()

    // 3. Blocked numbers
    private val _blockUnknownNumbers = MutableStateFlow(prefs.getBoolean("block_unknown", false))
    val blockUnknownNumbers: StateFlow<Boolean> = _blockUnknownNumbers.asStateFlow()

    // 4. Calling accounts
    private val _callingAccountChoice = MutableStateFlow(prefs.getString("calling_account_choice", "Ask every time") ?: "Ask every time")
    val callingAccountChoice: StateFlow<String> = _callingAccountChoice.asStateFlow()

    // 5. Display options
    private val _sortOrder = MutableStateFlow(prefs.getString("sort_order", "First name") ?: "First name")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    private val _nameFormat = MutableStateFlow(prefs.getString("name_format", "First name first") ?: "First name first")
    val nameFormat: StateFlow<String> = _nameFormat.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "Dark") ?: "Dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // 6. Incoming call gesture
    private val _gestureFlipSilence = MutableStateFlow(prefs.getBoolean("gesture_flip_silence", true))
    val gestureFlipSilence: StateFlow<Boolean> = _gestureFlipSilence.asStateFlow()

    private val _gesturePickupReduce = MutableStateFlow(prefs.getBoolean("gesture_pickup_reduce", true))
    val gesturePickupReduce: StateFlow<Boolean> = _gesturePickupReduce.asStateFlow()

    // 7. Sounds and vibration
    private val _vibrateForCalls = MutableStateFlow(prefs.getBoolean("vibrate_for_calls", true))
    val vibrateForCalls: StateFlow<Boolean> = _vibrateForCalls.asStateFlow()

    private val _dialpadTones = MutableStateFlow(prefs.getBoolean("dialpad_tones", true))
    val dialpadTones: StateFlow<Boolean> = _dialpadTones.asStateFlow()

    private val _callEndTone = MutableStateFlow(prefs.getBoolean("call_end_tone", true))
    val callEndTone: StateFlow<Boolean> = _callEndTone.asStateFlow()

    private val _ringtoneName = MutableStateFlow(prefs.getString("ringtone_name", "Cyber Minimal (Default)") ?: "Cyber Minimal (Default)")
    val ringtoneName: StateFlow<String> = _ringtoneName.asStateFlow()

    // 8. Voicemail
    private val _voicemailNumber = MutableStateFlow(prefs.getString("voicemail_number", "*86") ?: "*86")
    val voicemailNumber: StateFlow<String> = _voicemailNumber.asStateFlow()

    private val _voicemailAlerts = MutableStateFlow(prefs.getBoolean("voicemail_alerts", true))
    val voicemailAlerts: StateFlow<Boolean> = _voicemailAlerts.asStateFlow()

    // 9. Contact ringtones
    private val _contactRingtones = MutableStateFlow(prefs.getBoolean("contact_ringtones", true))
    val contactRingtones: StateFlow<Boolean> = _contactRingtones.asStateFlow()

    // 10. Calling card
    private val _callingCardEnabled = MutableStateFlow(prefs.getBoolean("calling_card_enabled", false))
    val callingCardEnabled: StateFlow<Boolean> = _callingCardEnabled.asStateFlow()

    private val _callingCardNumber = MutableStateFlow(prefs.getString("calling_card_number", "") ?: "")
    val callingCardNumber: StateFlow<String> = _callingCardNumber.asStateFlow()

    private val _callingCardPin = MutableStateFlow(prefs.getString("calling_card_pin", "") ?: "")
    val callingCardPin: StateFlow<String> = _callingCardPin.asStateFlow()

    // 11. Advanced: Caller ID announcement
    private val _callerIdAnnouncement = MutableStateFlow(prefs.getString("caller_id_announcement", "Only when using a headset") ?: "Only when using a headset")
    val callerIdAnnouncement: StateFlow<String> = _callerIdAnnouncement.asStateFlow()

    // 12. Advanced: Flip to Silence
    private val _flipToSilence = MutableStateFlow(prefs.getBoolean("flip_to_silence", true))
    val flipToSilence: StateFlow<Boolean> = _flipToSilence.asStateFlow()

    // Mutator functions
    fun setTtyMode(mode: String) {
        prefs.edit().putString("tty_mode", mode).apply()
        _ttyMode.value = mode
    }

    fun setHearingAid(enabled: Boolean) {
        prefs.edit().putBoolean("hearing_aid", enabled).apply()
        _hearingAid.value = enabled
    }

    fun setAssistedDialing(enabled: Boolean) {
        prefs.edit().putBoolean("assisted_dialing", enabled).apply()
        _assistedDialing.value = enabled
    }

    fun setDefaultCountry(country: String) {
        prefs.edit().putString("default_country", country).apply()
        _defaultCountry.value = country
    }

    fun setBlockUnknownNumbers(enabled: Boolean) {
        prefs.edit().putBoolean("block_unknown", enabled).apply()
        _blockUnknownNumbers.value = enabled
    }

    fun setCallingAccountChoice(choice: String) {
        prefs.edit().putString("calling_account_choice", choice).apply()
        _callingAccountChoice.value = choice
    }

    fun setSortOrder(order: String) {
        prefs.edit().putString("sort_order", order).apply()
        _sortOrder.value = order
    }

    fun setNameFormat(format: String) {
        prefs.edit().putString("name_format", format).apply()
        _nameFormat.value = format
    }

    fun setThemeMode(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
        _themeMode.value = theme
    }

    fun setGestureFlipSilence(enabled: Boolean) {
        prefs.edit().putBoolean("gesture_flip_silence", enabled).apply()
        _gestureFlipSilence.value = enabled
    }

    fun setGesturePickupReduce(enabled: Boolean) {
        prefs.edit().putBoolean("gesture_pickup_reduce", enabled).apply()
        _gesturePickupReduce.value = enabled
    }

    fun setVibrateForCalls(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_for_calls", enabled).apply()
        _vibrateForCalls.value = enabled
    }

    fun setDialpadTones(enabled: Boolean) {
        prefs.edit().putBoolean("dialpad_tones", enabled).apply()
        _dialpadTones.value = enabled
    }

    fun setCallEndTone(enabled: Boolean) {
        prefs.edit().putBoolean("call_end_tone", enabled).apply()
        _callEndTone.value = enabled
    }

    fun setRingtoneName(name: String) {
        prefs.edit().putString("ringtone_name", name).apply()
        _ringtoneName.value = name
    }

    fun setVoicemailNumber(num: String) {
        prefs.edit().putString("voicemail_number", num).apply()
        _voicemailNumber.value = num
    }

    fun setVoicemailAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("voicemail_alerts", enabled).apply()
        _voicemailAlerts.value = enabled
    }

    fun setContactRingtones(enabled: Boolean) {
        prefs.edit().putBoolean("contact_ringtones", enabled).apply()
        _contactRingtones.value = enabled
    }

    fun setCallingCardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("calling_card_enabled", enabled).apply()
        _callingCardEnabled.value = enabled
    }

    fun setCallingCardDetails(number: String, pin: String) {
        prefs.edit().putString("calling_card_number", number).putString("calling_card_pin", pin).apply()
        _callingCardNumber.value = number
        _callingCardPin.value = pin
    }

    fun setCallerIdAnnouncement(mode: String) {
        prefs.edit().putString("caller_id_announcement", mode).apply()
        _callerIdAnnouncement.value = mode
    }

    fun setFlipToSilence(enabled: Boolean) {
        prefs.edit().putBoolean("flip_to_silence", enabled).apply()
        _flipToSilence.value = enabled
    }
}
