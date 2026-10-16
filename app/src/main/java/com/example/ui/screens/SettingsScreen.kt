package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallRecordingEntity
import com.example.data.model.OverlayMode
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsSubScreen {
    MAIN,
    CALL_DISPLAY_MODE,
    BLOCKED_NUMBERS,
    CALL_RECORDING,
    SPAM_CALLER_ID,
    SOUNDS_VIBRATION,
    DISPLAY_OPTIONS,
    INCOMING_GESTURES,
    QUICK_RESPONSES,
    CALLING_ACCOUNTS,
    VOICEMAIL,
    ACCESSIBILITY,
    CONTACT_RINGTONES
}

@Composable
fun SettingsScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Intercept back button when inside a sub-screen to return to main Settings menu
    BackHandler(enabled = activeSubScreen != SettingsSubScreen.MAIN) {
        activeSubScreen = SettingsSubScreen.MAIN
    }

    when (activeSubScreen) {
        SettingsSubScreen.MAIN -> {
            MainSettingsList(
                viewModel = viewModel,
                onBack = onBack,
                onNavigate = { activeSubScreen = it },
                modifier = modifier
            )
        }
        SettingsSubScreen.CALL_DISPLAY_MODE -> {
            CallDisplayModeSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.BLOCKED_NUMBERS -> {
            BlockedNumbersSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.CALL_RECORDING -> {
            CallRecordingSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.SPAM_CALLER_ID -> {
            SpamCallerIdSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.SOUNDS_VIBRATION -> {
            SoundsVibrationSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.DISPLAY_OPTIONS -> {
            DisplayOptionsSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.INCOMING_GESTURES -> {
            IncomingGesturesSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.QUICK_RESPONSES -> {
            QuickResponsesSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.CALLING_ACCOUNTS -> {
            CallingAccountsSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.VOICEMAIL -> {
            VoicemailSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.ACCESSIBILITY -> {
            AccessibilitySubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubScreen.CONTACT_RINGTONES -> {
            ContactRingtonesSubScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = SettingsSubScreen.MAIN },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeobrutalIconButton(
            onClick = onBack,
            containerColor = NeobrutalWhite,
            size = 40.dp,
            shadowOffset = 3.dp,
            testTag = "settings_back_btn",
            icon = {
                Text(
                    text = "<",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = NeobrutalBlack
                    )
                )
            }
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeobrutalBlack,
                letterSpacing = 0.5.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun MainSettingsList(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    onNavigate: (SettingsSubScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "SETTINGS", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // General Category
            item {
                Row(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                    NeobrutalBadge(text = "GENERAL", color = NeobrutalYellow)
                }
            }

            // 1. Default Phone App
            item {
                val isDefault by viewModel.isDefaultDialer.collectAsState()
                val roleLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.updatePermissionStates()
                }

                NeobrutalSettingsCard(
                    icon = Icons.Default.PhoneInTalk,
                    title = "DEFAULT PHONE APP",
                    subtitle = if (isDefault) "Active • Managing calls, recordings & spam" else "Tap to set Brutal Dial as default dialer",
                    accentBadge = if (isDefault) "DEFAULT" else "NOT SET",
                    onClick = {
                        val intent = viewModel.getDefaultDialerIntent()
                        if (intent != null) {
                            roleLauncher.launch(intent)
                        } else {
                            viewModel.updatePermissionStates()
                        }
                    },
                    testTag = "setting_default_phone_app"
                )
            }

            // 2. Receive Incoming Call (Simulation / Test Call)
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.PhoneCallback,
                    title = "RECEIVE INCOMING CALL",
                    subtitle = "Trigger test call to test answer, decline, HUD & audio",
                    accentBadge = "TEST CALL",
                    onClick = {
                        viewModel.closeSettings()
                        viewModel.simulateIncomingCall()
                    },
                    testTag = "setting_test_incoming_call"
                )
            }

            // 3. Mini Pop-up & Call Display
            item {
                val mode by viewModel.callDisplayMode.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Fullscreen,
                    title = "MINI POP-UP & CALL DISPLAY",
                    subtitle = "Floating card, compact banner & full screen HUD modes",
                    accentBadge = mode.name.replace("_", " "),
                    onClick = { onNavigate(SettingsSubScreen.CALL_DISPLAY_MODE) },
                    testTag = "setting_call_display_mode"
                )
            }

            // 4. Blocked Numbers & Spam
            item {
                val blockUnknown by viewModel.blockUnknownNumbers.collectAsState()
                val blockedRules by viewModel.allBlockedRules.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Block,
                    title = "BLOCKED NUMBERS",
                    subtitle = "${blockedRules.size} blocked • Unknown numbers ${if (blockUnknown) "blocked" else "allowed"}",
                    onClick = { onNavigate(SettingsSubScreen.BLOCKED_NUMBERS) },
                    testTag = "setting_blocked_numbers"
                )
            }

            // 5. Call Recording (Added full-screen management!)
            item {
                val autoRecord by viewModel.autoRecordCalls.collectAsState()
                val recordings by viewModel.allRecordings.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Mic,
                    title = "CALL RECORDING",
                    subtitle = "Auto-record ${if (autoRecord) "ON" else "OFF"} • ${recordings.size} saved recordings",
                    accentBadge = if (autoRecord) "AUTO" else null,
                    onClick = { onNavigate(SettingsSubScreen.CALL_RECORDING) },
                    testTag = "setting_call_recording"
                )
            }

            // 6. Caller ID & Spam Shield
            item {
                val spamFilter by viewModel.callerIdSpamFilter.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Security,
                    title = "CALLER ID & SPAM PROTECTION",
                    subtitle = "Real-time caller lookup • Spam filter ${if (spamFilter) "ACTIVE" else "OFF"}",
                    accentBadge = if (spamFilter) "PROTECTED" else null,
                    onClick = { onNavigate(SettingsSubScreen.SPAM_CALLER_ID) },
                    testTag = "setting_spam_caller_id"
                )
            }

            // Preferences Category
            item {
                Row(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                    NeobrutalBadge(text = "PREFERENCES", color = NeobrutalCyan)
                }
            }

            // 7. Sounds and Vibration
            item {
                val ringtone by viewModel.ringtoneName.collectAsState()
                val vibrate by viewModel.vibrateForCalls.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.VolumeUp,
                    title = "SOUNDS AND VIBRATION",
                    subtitle = "$ringtone • Vibrate ${if (vibrate) "ON" else "OFF"}",
                    onClick = { onNavigate(SettingsSubScreen.SOUNDS_VIBRATION) },
                    testTag = "setting_sounds_vibration"
                )
            }

            // 8. Display Options
            item {
                val sortOrder by viewModel.sortOrder.collectAsState()
                val theme by viewModel.themeMode.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.FormatListBulleted,
                    title = "DISPLAY OPTIONS",
                    subtitle = "Sort by $sortOrder • Theme: $theme",
                    onClick = { onNavigate(SettingsSubScreen.DISPLAY_OPTIONS) },
                    testTag = "setting_display_options"
                )
            }

            // 9. Incoming Call Gestures & Hardware Buttons
            item {
                val flipSilence by viewModel.gestureFlipSilence.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.TouchApp,
                    title = "INCOMING CALL GESTURES",
                    subtitle = "Flip to silence ${if (flipSilence) "ON" else "OFF"} • Power & volume controls",
                    onClick = { onNavigate(SettingsSubScreen.INCOMING_GESTURES) },
                    testTag = "setting_incoming_gestures"
                )
            }

            // 10. Quick Responses
            item {
                val responses by viewModel.quickResponses.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "QUICK RESPONSES",
                    subtitle = "${responses.size} canned SMS decline templates",
                    onClick = { onNavigate(SettingsSubScreen.QUICK_RESPONSES) },
                    testTag = "setting_quick_responses"
                )
            }

            // Advanced Category
            item {
                Row(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                    NeobrutalBadge(text = "ADVANCED", color = NeobrutalPink, textColor = NeobrutalWhite)
                }
            }

            // 11. Calling Accounts & Dual SIM
            item {
                val choice by viewModel.callingAccountChoice.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.ContactPhone,
                    title = "CALLING ACCOUNTS",
                    subtitle = "SIM preference: $choice • SIP VoIP settings",
                    onClick = { onNavigate(SettingsSubScreen.CALLING_ACCOUNTS) },
                    testTag = "setting_calling_accounts"
                )
            }

            // 12. Voicemail
            item {
                val vmNumber by viewModel.voicemailNumber.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Voicemail,
                    title = "VOICEMAIL",
                    subtitle = "Access number: $vmNumber • Notifications & alerts",
                    onClick = { onNavigate(SettingsSubScreen.VOICEMAIL) },
                    testTag = "setting_voicemail"
                )
            }

            // 13. Accessibility
            item {
                val hearingAid by viewModel.hearingAid.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Accessibility,
                    title = "ACCESSIBILITY",
                    subtitle = "Hearing aid ${if (hearingAid) "ON" else "OFF"} • TTY • Caller announcer",
                    onClick = { onNavigate(SettingsSubScreen.ACCESSIBILITY) },
                    testTag = "setting_accessibility"
                )
            }

            // 14. Contact Ringtones
            item {
                val crEnabled by viewModel.contactRingtones.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.MusicNote,
                    title = "CONTACT RINGTONES",
                    subtitle = "Custom ringtones per person: ${if (crEnabled) "ACTIVE" else "OFF"}",
                    onClick = { onNavigate(SettingsSubScreen.CONTACT_RINGTONES) },
                    testTag = "setting_contact_ringtones"
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun NeobrutalSettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentBadge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    NeobrutalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NeobrutalWhite,
        shadowOffset = 3.dp,
        borderWidth = 3.dp,
        testTag = testTag,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(NeobrutalYellow)
                    .border(BorderStroke(2.dp, NeobrutalBlack)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NeobrutalBlack,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = NeobrutalBlack
                    )
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = Color(0xFF555555)
                    ),
                    maxLines = 1
                )
            }

            if (accentBadge != null) {
                NeobrutalBadge(
                    text = accentBadge,
                    color = NeobrutalCyan
                )
            } else {
                Text(
                    text = ">",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeobrutalBlack
                    ),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SUB-SCREENS: Full Screen Setting Experiences
// -------------------------------------------------------------------------------------------------

@Composable
private fun CallDisplayModeSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.callDisplayMode.collectAsState()
    val canOverlay by viewModel.canDrawOverlays.collectAsState()
    val keepAfterAnswer by viewModel.miniPopupKeepAfterAnswer.collectAsState()
    val autoMinimize by viewModel.miniPopupAutoMinimize.collectAsState()
    val showAvatar by viewModel.miniPopupShowAvatar.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "MINI POP-UP & DISPLAY", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Banner if Pop-up or Mini mode selected but overlay permission not granted
            if (!canOverlay && (currentMode == OverlayMode.POP_UP || currentMode == OverlayMode.MINI_POP_UP)) {
                item {
                    NeobrutalCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NeobrutalPink,
                        shadowOffset = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = NeobrutalWhite, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DISPLAY OVER OTHER APPS REQUIRED",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalWhite)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To show the floating card or mini multitask banner while you use other apps, enable 'Display over other apps'.",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalWhite)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            NeobrutalButton(
                                onClick = {
                                    try {
                                        val intent = viewModel.getOverlaySettingsIntent()
                                        context.startActivity(intent)
                                    } catch (_: Throwable) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                        } catch (_: Throwable) {}
                                    }
                                },
                                text = "ALLOW ON OTHER APPS",
                                containerColor = NeobrutalYellow,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shadowOffset = 2.dp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "SELECT IN-CALL HUD STYLE:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            // Modes
            val modes = listOf(
                Triple(OverlayMode.FULL_SCREEN, "FULL SCREEN", "Traditional immersive in-call dialer experience with large action controls."),
                Triple(OverlayMode.POP_UP, "POP-UP", "Floating Neobrutal card that appears while you are using other apps."),
                Triple(OverlayMode.MINI_POP_UP, "MINI POP-UP", "Ultra-compact multitasking pill banner anchored at the top of the screen.")
            )

            items(modes) { (mode, title, desc) ->
                NeobrutalCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (currentMode == mode) NeobrutalYellow else NeobrutalWhite,
                    shadowOffset = if (currentMode == mode) 4.dp else 2.dp,
                    onClick = {
                        viewModel.setCallDisplayMode(mode)
                        if (!canOverlay && (mode == OverlayMode.POP_UP || mode == OverlayMode.MINI_POP_UP)) {
                            try {
                                val intent = viewModel.getOverlaySettingsIntent()
                                context.startActivity(intent)
                            } catch (_: Throwable) {}
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeobrutalRadioButton(
                            selected = currentMode == mode,
                            onClick = { viewModel.setCallDisplayMode(mode) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = NeobrutalBlack)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(desc, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MINI POP-UP MULTITASKING OPTIONS:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("KEEP IN POP-UP AFTER ANSWER", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Stay in compact overlay rather than opening full screen", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = keepAfterAnswer, onCheckedChange = { viewModel.setMiniPopupKeepAfterAnswer(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AUTO-MINIMIZE ACTIVE CALLS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Shrink to minimal pill when speech is detected", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = autoMinimize, onCheckedChange = { viewModel.setMiniPopupAutoMinimize(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SHOW CALLER AVATAR IN PILL", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Display contact initials and neon color tag", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = showAvatar, onCheckedChange = { viewModel.setMiniPopupShowAvatar(it) })
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BlockedNumbersSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blockUnknown by viewModel.blockUnknownNumbers.collectAsState()
    val blockedRules by viewModel.allBlockedRules.collectAsState()
    var newNumber by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "BLOCKED NUMBERS", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BLOCK UNIDENTIFIED NUMBERS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                            Text("Reject calls from private and unregistered numbers automatically", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                        }
                        NeobrutalSwitch(checked = blockUnknown, onCheckedChange = { viewModel.setBlockUnknown(it) })
                    }
                }
            }

            item {
                Text(
                    text = "ADD NUMBER TO BLOCK LIST:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalTextField(
                        value = newNumber,
                        onValueChange = { newNumber = it },
                        placeholder = "ENTER PHONE NUMBER...",
                        modifier = Modifier.weight(1f)
                    )
                    NeobrutalButton(
                        onClick = {
                            if (newNumber.isNotBlank()) {
                                viewModel.addBlockedRule(newNumber.trim(), false, "Manual block list")
                                newNumber = ""
                            }
                        },
                        text = "BLOCK",
                        containerColor = NeobrutalPink,
                        contentColor = NeobrutalWhite,
                        modifier = Modifier.height(52.dp),
                        shadowOffset = 3.dp
                    )
                }
            }

            item {
                Text(
                    text = "BLOCKED CONTACTS & RULES (${blockedRules.size}):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            if (blockedRules.isEmpty()) {
                item {
                    NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("NO NUMBERS BLOCKED YET", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(blockedRules, key = { it.id }) { rule ->
                    NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rule.pattern, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = NeobrutalBlack)
                                Text(rule.note.ifEmpty { "Blocked number" }, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalButton(
                                onClick = { viewModel.deleteBlockedRule(rule) },
                                text = "UNBLOCK",
                                containerColor = NeobrutalWhite,
                                contentColor = Color.Red,
                                modifier = Modifier.height(34.dp),
                                shadowOffset = 2.dp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CallRecordingSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val autoRecord by viewModel.autoRecordCalls.collectAsState()
    val recordUnknownOnly by viewModel.recordUnknownOnly.collectAsState()
    val recordingQuality by viewModel.recordingQuality.collectAsState()
    val allRecordings by viewModel.allRecordings.collectAsState()
    val playbackState by viewModel.recordingPlaybackState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "CALL RECORDING", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AUTO-RECORD ALL CALLS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Record calls automatically without needing to press record", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(
                                checked = autoRecord,
                                onCheckedChange = { viewModel.preferences.setAutoRecordCalls(it) }
                            )
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RECORD UNKNOWN NUMBERS ONLY", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Exclude calls from saved phonebook contacts", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(
                                checked = recordUnknownOnly,
                                onCheckedChange = { viewModel.preferences.setRecordUnknownOnly(it) }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "RECORDING AUDIO QUALITY:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Standard (64kbps AAC)",
                            "High Quality (128kbps AAC)",
                            "Studio Voice (256kbps AAC)"
                        ).forEach { q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.preferences.setRecordingQuality(q) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(
                                    selected = recordingQuality == q,
                                    onClick = { viewModel.preferences.setRecordingQuality(q) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(q, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "SAVED CALL RECORDINGS (${allRecordings.size}):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            if (allRecordings.isEmpty()) {
                item {
                    NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("NO CALL RECORDINGS STORED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(allRecordings, key = { it.id }) { rec ->
                    val isPlaying = playbackState.recordingId == rec.id && playbackState.isPlaying
                    val formattedDate = remember(rec.timestamp) {
                        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(rec.timestamp))
                    }

                    NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.callerName.ifEmpty { rec.callerNumber }, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = NeobrutalBlack)
                                    Text("${rec.callerNumber} • $formattedDate", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                                    Text("Duration: ${rec.durationSeconds}s", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeobrutalCyan)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NeobrutalIconButton(
                                        onClick = {
                                            if (isPlaying) viewModel.stopRecordingPlayback() else viewModel.playRecording(rec)
                                        },
                                        containerColor = if (isPlaying) NeobrutalPink else NeobrutalGreen,
                                        size = 36.dp,
                                        icon = {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = NeobrutalBlack,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )

                                    NeobrutalIconButton(
                                        onClick = { viewModel.deleteRecording(rec) },
                                        containerColor = NeobrutalWhite,
                                        size = 36.dp,
                                        icon = {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SpamCallerIdSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spamFilter by viewModel.callerIdSpamFilter.collectAsState()
    val blockSpam by viewModel.blockSpamCalls.collectAsState()
    val verifiedCalls by viewModel.verifiedCalls.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "CALLER ID & SPAM", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CALLER ID & SPAM LOOKUP", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Identify business names and spam callers in real time", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(
                                checked = spamFilter,
                                onCheckedChange = { viewModel.preferences.setCallerIdSpamFilter(it) }
                            )
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FILTER SUSPECTED SPAM CALLS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Prevent suspected spam calls from ringing on your device", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(
                                checked = blockSpam,
                                onCheckedChange = { viewModel.preferences.setBlockSpamCalls(it) }
                            )
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VERIFIED CALLS BADGE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Show green verified checkmark and reason for official business calls", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(
                                checked = verifiedCalls,
                                onCheckedChange = { viewModel.preferences.setVerifiedCalls(it) }
                            )
                        }
                    }
                }
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalYellow) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("HOW SPAM PROTECTION WORKS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When an incoming call arrives, Brutal Dial checks the caller's number against your local block rules and carrier network database. Flagged numbers display a prominent pink SPAM warning banner on incoming HUD.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NeobrutalBlack
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SoundsVibrationSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringtoneName by viewModel.ringtoneName.collectAsState()
    val vibrate by viewModel.vibrateForCalls.collectAsState()
    val dialpadTones by viewModel.dialpadTones.collectAsState()
    val callEndTone by viewModel.callEndTone.collectAsState()
    val vibrateOnConnect by viewModel.vibrateOnConnect.collectAsState()
    val vibrateOnDisconnect by viewModel.vibrateOnDisconnect.collectAsState()
    val hapticStrength by viewModel.dialpadHapticStrength.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "SOUNDS & VIBRATION", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "PHONE RINGTONE:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Cyber Minimal (Default)",
                            "Retro Brutal Bell",
                            "Electric Shockwave",
                            "Minimal Clean Click",
                            "Industrial Pulse",
                            "Digital Synthesizer"
                        ).forEach { rName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setRingtoneName(rName) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(
                                    selected = ringtoneName == rName,
                                    onClick = { viewModel.setRingtoneName(rName) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(rName, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "VIBRATION & CALL HAPTICS:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VIBRATE FOR CALLS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Vibrate device while ringer is sounding", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = vibrate, onCheckedChange = { viewModel.setVibrateForCalls(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VIBRATE ON CALL CONNECT", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Gentle tactile pulse when other party answers", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = vibrateOnConnect, onCheckedChange = { viewModel.preferences.setVibrateOnConnect(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VIBRATE ON CALL DISCONNECT", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Haptic alert when the call terminates", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = vibrateOnDisconnect, onCheckedChange = { viewModel.preferences.setVibrateOnDisconnect(it) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "AUDIO TONES & KEYPAD:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DIAL PAD TONES", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Play DTMF tones when dialing numbers", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = dialpadTones, onCheckedChange = { viewModel.setDialpadTones(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CALL END TONE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                                Text("Audible chime when call is ended", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = callEndTone, onCheckedChange = { viewModel.setCallEndTone(it) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "DIALPAD HAPTIC FEEDBACK STRENGTH:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Strong (Neobrutal)",
                            "Medium Tactile",
                            "Light Click",
                            "Off (Silent)"
                        ).forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.preferences.setDialpadHapticStrength(s) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(
                                    selected = hapticStrength == s,
                                    onClick = { viewModel.preferences.setDialpadHapticStrength(s) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(s, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DisplayOptionsSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortOrder by viewModel.sortOrder.collectAsState()
    val nameFormat by viewModel.nameFormat.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "DISPLAY OPTIONS", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "CONTACTS SORT ORDER:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("First name", "Surname (Last name)").forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSortOrder(opt) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(selected = sortOrder == opt, onClick = { viewModel.setSortOrder(opt) })
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(opt, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "NAME DISPLAY FORMAT:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("First name first", "Surname first").forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setNameFormat(opt) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(selected = nameFormat == opt, onClick = { viewModel.setNameFormat(opt) })
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(opt, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "THEME & VISUAL STYLE:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Light (Neobrutal Paper)",
                            "Cyber Dark",
                            "High Contrast Cyber Yellow"
                        ).forEach { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setThemeMode(t) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(selected = themeMode == t, onClick = { viewModel.setThemeMode(t) })
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(t, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun IncomingGesturesSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flipSilence by viewModel.gestureFlipSilence.collectAsState()
    val pickupReduce by viewModel.gesturePickupReduce.collectAsState()
    val powerEndsCall by viewModel.powerButtonEndsCall.collectAsState()
    val volumeAnswers by viewModel.volumeKeyAnswersCall.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "CALL GESTURES & KEYS", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "MOTION SENSOR GESTURES:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FLIP TO SILENCE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Turn phone face down on flat surface to silence incoming ringer", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = flipSilence, onCheckedChange = { viewModel.setGestureFlipSilence(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PICK UP TO REDUCE VOLUME", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Lower ringer volume automatically when device is lifted", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = pickupReduce, onCheckedChange = { viewModel.setGesturePickupReduce(it) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "HARDWARE BUTTON ACTIONS:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("POWER BUTTON ENDS CALL", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Press hardware power button to hang up active call", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = powerEndsCall, onCheckedChange = { viewModel.preferences.setPowerButtonEndsCall(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VOLUME UP ANSWERS CALL", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Press hardware Volume Up button to answer ringing call", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = volumeAnswers, onCheckedChange = { viewModel.preferences.setVolumeKeyAnswersCall(it) })
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickResponsesSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responses by viewModel.quickResponses.collectAsState()
    var newResponseText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "QUICK RESPONSES", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "ADD NEW QUICK REJECTION SMS:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalTextField(
                        value = newResponseText,
                        onValueChange = { newResponseText = it },
                        placeholder = "I'LL CALL YOU RIGHT BACK...",
                        modifier = Modifier.weight(1f)
                    )
                    NeobrutalButton(
                        onClick = {
                            if (newResponseText.isNotBlank()) {
                                viewModel.addQuickResponse(newResponseText.trim())
                                newResponseText = ""
                            }
                        },
                        text = "ADD",
                        containerColor = NeobrutalCyan,
                        modifier = Modifier.height(52.dp),
                        shadowOffset = 3.dp
                    )
                }
            }

            item {
                Text(
                    text = "CANNED SMS DECLINE MESSAGES (${responses.size}):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            items(responses, key = { it.id }) { resp ->
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = resp.text,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { viewModel.deleteQuickResponse(resp) }
                        )
                    }
                }
            }

            item {
                NeobrutalButton(
                    onClick = { viewModel.restoreDefaultQuickResponses() },
                    text = "RESTORE DEFAULT RESPONSES",
                    containerColor = NeobrutalYellow,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shadowOffset = 3.dp
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CallingAccountsSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val choice by viewModel.callingAccountChoice.collectAsState()
    val sipEnabled by viewModel.sipCallingEnabled.collectAsState()
    val receiveSip by viewModel.receiveSipCalls.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "CALLING ACCOUNTS", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "DEFAULT OUTGOING CALL ACCOUNT:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Ask every time",
                            "SIM 1 (Carrier Primary)",
                            "SIM 2 (Secondary SIM)"
                        ).forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setCallingAccountChoice(opt) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(selected = choice == opt, onClick = { viewModel.setCallingAccountChoice(opt) })
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(opt, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "INTERNET CALLING (SIP / VOIP):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SIP CALLING SUPPORT", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Place calls through internet telephony accounts", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = sipEnabled, onCheckedChange = { viewModel.preferences.setSipCallingEnabled(it) })
                        }

                        HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RECEIVE INCOMING SIP CALLS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                                Text("Listen for incoming calls on registered SIP profiles", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalSwitch(checked = receiveSip, onCheckedChange = { viewModel.preferences.setReceiveSipCalls(it) })
                        }
                    }
                }
            }

            item {
                NeobrutalButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        } catch (_: Throwable) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            } catch (_: Throwable) {}
                        }
                    },
                    text = "OPEN SYSTEM SIM & NETWORK SETTINGS",
                    containerColor = NeobrutalYellow,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shadowOffset = 3.dp
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun VoicemailSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val number by viewModel.voicemailNumber.collectAsState()
    val alerts by viewModel.voicemailAlerts.collectAsState()
    var numInput by remember { mutableStateOf(number) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "VOICEMAIL SETUP", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "CARRIER VOICEMAIL NUMBER:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalTextField(
                        value = numInput,
                        onValueChange = { numInput = it },
                        placeholder = "*86 OR 123",
                        modifier = Modifier.weight(1f)
                    )
                    NeobrutalButton(
                        onClick = {
                            viewModel.setVoicemailNumber(numInput.trim())
                        },
                        text = "SAVE",
                        containerColor = NeobrutalGreen,
                        modifier = Modifier.height(52.dp),
                        shadowOffset = 3.dp
                    )
                }
            }

            item {
                NeobrutalButton(
                    onClick = {
                        val target = numInput.ifEmpty { number }
                        if (target.isNotBlank()) {
                            viewModel.closeSettings()
                            viewModel.startCall(target)
                        }
                    },
                    text = "CALL VOICEMAIL NOW",
                    containerColor = NeobrutalCyan,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shadowOffset = 3.dp
                )
            }

            item {
                Text(
                    text = "NOTIFICATIONS & NOTIFICATIONS:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VOICEMAIL NOTIFICATIONS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                            Text("Notify when new voicemail messages are received", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                        }
                        NeobrutalSwitch(checked = alerts, onCheckedChange = { viewModel.setVoicemailAlerts(it) })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AccessibilitySubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ttyMode by viewModel.ttyMode.collectAsState()
    val hearingAid by viewModel.hearingAid.collectAsState()
    val callerAnnouncement by viewModel.callerIdAnnouncement.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "ACCESSIBILITY", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "TTY (TELETYPEWRITER) MODE:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OFF", "TTY FULL", "TTY HCO", "TTY VCO").forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setTtyMode(mode) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(selected = ttyMode == mode, onClick = { viewModel.setTtyMode(mode) })
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(mode, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "HEARING AID COMPATIBILITY:",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HEARING AID COMPATIBILITY", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                            Text("Optimize telecoil acoustic coupling for hearing devices", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                        }
                        NeobrutalSwitch(checked = hearingAid, onCheckedChange = { viewModel.setHearingAid(it) })
                    }
                }
            }

            item {
                Text(
                    text = "ANNOUNCE CALLER NAMES (TEXT-TO-SPEECH):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Always",
                            "Only when using a headset",
                            "Never"
                        ).forEach { ann ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setCallerIdAnnouncement(ann) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeobrutalRadioButton(
                                    selected = callerAnnouncement == ann,
                                    onClick = { viewModel.setCallerIdAnnouncement(ann) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ann, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeobrutalBlack)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ContactRingtonesSubScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled by viewModel.contactRingtones.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "CONTACT RINGTONES", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CUSTOM CONTACT RINGTONES", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                            Text("Play personalized ringtones assigned to specific contacts", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                        }
                        NeobrutalSwitch(checked = enabled, onCheckedChange = { viewModel.setContactRingtones(it) })
                    }
                }
            }

            item {
                Text(
                    text = "CONTACTS & ASSIGNED TONES (${allContacts.size}):",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeobrutalBlack)
                )
            }

            if (allContacts.isEmpty()) {
                item {
                    NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("NO CONTACTS TO CONFIGURE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(allContacts.take(15), key = { "${it.id}_${it.lookupKey}_${it.phoneNumber}" }) { contact ->
                    NeobrutalCard(modifier = Modifier.fillMaxWidth(), containerColor = NeobrutalWhite) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(contact.displayName, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = NeobrutalBlack)
                                Text(contact.phoneNumber, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                            }
                            NeobrutalBadge(
                                text = "CYBER DEFAULT",
                                color = NeobrutalYellow
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
