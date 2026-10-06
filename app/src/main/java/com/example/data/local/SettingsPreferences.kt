package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AutoRecordRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("brutal_dial_prefs", Context.MODE_PRIVATE)

    private val _autoAnswerEnabled = MutableStateFlow(prefs.getBoolean("auto_answer", false))
    val autoAnswerEnabled: StateFlow<Boolean> = _autoAnswerEnabled.asStateFlow()

    private val _autoAnswerDelay = MutableStateFlow(prefs.getInt("auto_answer_delay", 3))
    val autoAnswerDelay: StateFlow<Int> = _autoAnswerDelay.asStateFlow()

    private val _autoAnswerHeadsetOnly = MutableStateFlow(prefs.getBoolean("auto_answer_headset_only", false))
    val autoAnswerHeadsetOnly: StateFlow<Boolean> = _autoAnswerHeadsetOnly.asStateFlow()

    private val _autoRecordRule = MutableStateFlow(
        AutoRecordRule.valueOf(prefs.getString("auto_record_rule", AutoRecordRule.OFF.name) ?: AutoRecordRule.OFF.name)
    )
    val autoRecordRule: StateFlow<AutoRecordRule> = _autoRecordRule.asStateFlow()

    private val _blockUnknownNumbers = MutableStateFlow(prefs.getBoolean("block_unknown", false))
    val blockUnknownNumbers: StateFlow<Boolean> = _blockUnknownNumbers.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setAutoAnswerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_answer", enabled).apply()
        _autoAnswerEnabled.value = enabled
    }

    fun setAutoAnswerDelay(seconds: Int) {
        prefs.edit().putInt("auto_answer_delay", seconds).apply()
        _autoAnswerDelay.value = seconds
    }

    fun setAutoAnswerHeadsetOnly(enabled: Boolean) {
        prefs.edit().putBoolean("auto_answer_headset_only", enabled).apply()
        _autoAnswerHeadsetOnly.value = enabled
    }

    fun setAutoRecordRule(rule: AutoRecordRule) {
        prefs.edit().putString("auto_record_rule", rule.name).apply()
        _autoRecordRule.value = rule
    }

    fun setBlockUnknownNumbers(enabled: Boolean) {
        prefs.edit().putBoolean("block_unknown", enabled).apply()
        _blockUnknownNumbers.value = enabled
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }
}
