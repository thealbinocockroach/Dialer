package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.OverlayMode
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel

private enum class SettingsDialogType {
    NONE,
    ACCESSIBILITY,
    ASSISTED_DIALING,
    BLOCKED_NUMBERS,
    CALLING_ACCOUNTS,
    DISPLAY_OPTIONS,
    INCOMING_GESTURE,
    QUICK_RESPONSES,
    SOUNDS_VIBRATION,
    VOICEMAIL,
    CONTACT_RINGTONES,
    CALLING_CARD,
    CALL_DISPLAY_MODE,
    CALLER_ID_ANNOUNCEMENT,
    FLIP_TO_SILENCE
}

@Composable
fun SettingsScreen(
    viewModel: DialerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDialog by remember { mutableStateOf(SettingsDialogType.NONE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .statusBarsPadding()
    ) {
        // --- Neobrutalist Top Bar with Back Arrow ---
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
                testTag = "settings_back_button",
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "SETTINGS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = NeobrutalBlack,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // --- Settings Items List in Neo-Brutalist Theme ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // General Category Header Chip
            item {
                Row(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                    NeobrutalBadge(
                        text = "GENERAL",
                        color = NeobrutalYellow
                    )
                }
            }

            // 1. Accessibility
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.Accessibility,
                    title = "ACCESSIBILITY",
                    subtitle = "TTY mode, hearing aid compatibility",
                    onClick = { activeDialog = SettingsDialogType.ACCESSIBILITY },
                    testTag = "setting_accessibility"
                )
            }

            // 2. Assisted dialing
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.Language,
                    title = "ASSISTED DIALING",
                    subtitle = "Prefixes for roaming & country code",
                    onClick = { activeDialog = SettingsDialogType.ASSISTED_DIALING },
                    testTag = "setting_assisted_dialing"
                )
            }

            // 3. Blocked numbers
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.Block,
                    title = "BLOCKED NUMBERS",
                    subtitle = "Block unidentified numbers & spam list",
                    onClick = { activeDialog = SettingsDialogType.BLOCKED_NUMBERS },
                    testTag = "setting_blocked_numbers"
                )
            }

            // 4. Calling accounts
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.ContactPhone,
                    title = "CALLING ACCOUNTS",
                    subtitle = "SIM card preferences & SIP VoIP",
                    onClick = { activeDialog = SettingsDialogType.CALLING_ACCOUNTS },
                    testTag = "setting_calling_accounts"
                )
            }

            // 5. Display options
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.FormatListBulleted,
                    title = "DISPLAY OPTIONS",
                    subtitle = "Sort by, name format, theme mode",
                    onClick = { activeDialog = SettingsDialogType.DISPLAY_OPTIONS },
                    testTag = "setting_display_options"
                )
            }

            // 6. In-call HUD display mode (Mini pop-up setting!)
            item {
                val mode by viewModel.callDisplayMode.collectAsState()
                NeobrutalSettingsCard(
                    icon = Icons.Default.Fullscreen,
                    title = "MINI POP-UP & CALL DISPLAY",
                    subtitle = "Floating mini pop-up, pop-up & full screen styles",
                    accentBadge = mode.name.replace("_", " "),
                    onClick = { activeDialog = SettingsDialogType.CALL_DISPLAY_MODE },
                    testTag = "setting_call_display_mode"
                )
            }

            // 7. Incoming call gesture
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.TouchApp,
                    title = "INCOMING CALL GESTURE",
                    subtitle = "Flip to silence, pick up volume reduce",
                    onClick = { activeDialog = SettingsDialogType.INCOMING_GESTURE },
                    testTag = "setting_incoming_gesture"
                )
            }

            // 8. Quick responses
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "QUICK RESPONSES",
                    subtitle = "Edit canned decline SMS templates",
                    onClick = { activeDialog = SettingsDialogType.QUICK_RESPONSES },
                    testTag = "setting_quick_responses"
                )
            }

            // 9. Sounds and vibration
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.VolumeUp,
                    title = "SOUNDS AND VIBRATION",
                    subtitle = "Ringtones, dial tones, vibration",
                    onClick = { activeDialog = SettingsDialogType.SOUNDS_VIBRATION },
                    testTag = "setting_sounds_vibration"
                )
            }

            // 10. Voicemail
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.Voicemail,
                    title = "VOICEMAIL",
                    subtitle = "Setup carrier voicemail number & alerts",
                    onClick = { activeDialog = SettingsDialogType.VOICEMAIL },
                    testTag = "setting_voicemail"
                )
            }

            // 11. Contact ringtones
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.MusicNote,
                    title = "CONTACT RINGTONES",
                    subtitle = "Assign custom ringtones per contact",
                    onClick = { activeDialog = SettingsDialogType.CONTACT_RINGTONES },
                    testTag = "setting_contact_ringtones"
                )
            }

            // 12. Calling card
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.CreditCard,
                    title = "CALLING CARD",
                    subtitle = "International PIN & prefix numbers",
                    onClick = { activeDialog = SettingsDialogType.CALLING_CARD },
                    testTag = "setting_calling_card"
                )
            }

            // Advanced Category Header Chip
            item {
                Row(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    NeobrutalBadge(
                        text = "ADVANCED",
                        color = NeobrutalPink,
                        textColor = NeobrutalWhite
                    )
                }
            }

            // 13. Caller ID announcement
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.Campaign,
                    title = "CALLER ID ANNOUNCEMENT",
                    subtitle = "Read caller names on incoming calls",
                    onClick = { activeDialog = SettingsDialogType.CALLER_ID_ANNOUNCEMENT },
                    testTag = "setting_caller_id"
                )
            }

            // 14. Flip to Silence
            item {
                NeobrutalSettingsCard(
                    icon = Icons.Default.ScreenRotation,
                    title = "FLIP TO SILENCE",
                    subtitle = "Turn phone face down to silence ringer",
                    onClick = { activeDialog = SettingsDialogType.FLIP_TO_SILENCE },
                    testTag = "setting_flip_silence"
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // --- Interactive Neobrutal Setting Dialogs ---
    when (activeDialog) {
        SettingsDialogType.ACCESSIBILITY -> {
            AccessibilitySettingsDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.ASSISTED_DIALING -> {
            AssistedDialingDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.BLOCKED_NUMBERS -> {
            BlockedNumbersDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.CALLING_ACCOUNTS -> {
            CallingAccountsDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.DISPLAY_OPTIONS -> {
            DisplayOptionsDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.CALL_DISPLAY_MODE -> {
            CallDisplayModeDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.INCOMING_GESTURE -> {
            IncomingCallGestureDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.QUICK_RESPONSES -> {
            QuickResponsesDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.SOUNDS_VIBRATION -> {
            SoundsVibrationDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.VOICEMAIL -> {
            VoicemailDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.CONTACT_RINGTONES -> {
            ContactRingtonesDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.CALLING_CARD -> {
            CallingCardDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.CALLER_ID_ANNOUNCEMENT -> {
            CallerIdAnnouncementDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.FLIP_TO_SILENCE -> {
            FlipToSilenceDialog(viewModel = viewModel, onDismiss = { activeDialog = SettingsDialogType.NONE })
        }
        SettingsDialogType.NONE -> {}
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
                    text = "->",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeobrutalBlack
                    )
                )
            }
        }
    }
}

// Setting: Call Display Mode (Full Screen, Pop-up, Mini Pop-up) & Mini Pop-up Options
@Composable
private fun CallDisplayModeDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val currentMode by viewModel.callDisplayMode.collectAsState()
    val keepAfterAnswer by viewModel.miniPopupKeepAfterAnswer.collectAsState()
    val autoMinimize by viewModel.miniPopupAutoMinimize.collectAsState()
    val showAvatar by viewModel.miniPopupShowAvatar.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "MINI POP-UP & DISPLAY", titleColor = NeobrutalCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CALL DISPLAY HUD MODE:",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, color = NeobrutalBlack)
            )

            listOf(
                Pair(OverlayMode.FULL_SCREEN, "FULL SCREEN - Classic in-app dialer HUD"),
                Pair(OverlayMode.POP_UP, "POP-UP - Floating card over app"),
                Pair(OverlayMode.MINI_POP_UP, "MINI POP-UP - Compact multitasking banner")
            ).forEach { (mode, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (currentMode == mode) NeobrutalYellow.copy(alpha = 0.35f) else NeobrutalWhite)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable { viewModel.setCallDisplayMode(mode) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(
                        selected = currentMode == mode,
                        onClick = { viewModel.setCallDisplayMode(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(mode.name.replace("_", " "), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                        Text(desc, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.DarkGray)
                    }
                }
            }

            HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

            Text(
                text = "MINI POP-UP OPTIONS:",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, color = NeobrutalBlack)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KEEP IN POP-UP AFTER ANSWER", color = NeobrutalBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                NeobrutalSwitch(checked = keepAfterAnswer, onCheckedChange = { viewModel.setMiniPopupKeepAfterAnswer(it) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AUTO-MINIMIZE ACTIVE CALLS", color = NeobrutalBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                NeobrutalSwitch(checked = autoMinimize, onCheckedChange = { viewModel.setMiniPopupAutoMinimize(it) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SHOW CALLER AVATAR IN PILL", color = NeobrutalBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                NeobrutalSwitch(checked = showAvatar, onCheckedChange = { viewModel.setMiniPopupShowAvatar(it) })
            }

            Spacer(modifier = Modifier.height(4.dp))
            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 1. Accessibility Dialog
@Composable
private fun AccessibilitySettingsDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val ttyMode by viewModel.ttyMode.collectAsState()
    val hearingAid by viewModel.hearingAid.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "ACCESSIBILITY", titleColor = NeobrutalYellow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("TTY MODE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
            listOf("OFF", "TTY FULL", "TTY HCO", "TTY VCO").forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTtyMode(mode) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(selected = ttyMode == mode, onClick = { viewModel.setTtyMode(mode) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(mode, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                }
            }

            HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HEARING AID COMPATIBILITY", color = NeobrutalBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                NeobrutalSwitch(checked = hearingAid, onCheckedChange = { viewModel.setHearingAid(it) })
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 2. Assisted Dialing Dialog
@Composable
private fun AssistedDialingDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val assisted by viewModel.assistedDialing.collectAsState()
    val country by viewModel.defaultCountry.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "ASSISTED DIALING", titleColor = NeobrutalCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ASSISTED DIALING", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                NeobrutalSwitch(checked = assisted, onCheckedChange = { viewModel.setAssistedDialing(it) })
            }

            Text("DEFAULT HOME COUNTRY", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
            listOf("United States (+1)", "United Kingdom (+44)", "Germany (+49)", "Japan (+81)").forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDefaultCountry(c) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(selected = country == c, onClick = { viewModel.setDefaultCountry(c) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(c, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                }
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "SAVE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 3. Blocked Numbers Dialog
@Composable
private fun BlockedNumbersDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val blockUnknown by viewModel.blockUnknownNumbers.collectAsState()
    val blockedRules by viewModel.allBlockedRules.collectAsState()
    var newNumber by remember { mutableStateOf("") }

    NeobrutalDialog(onDismissRequest = onDismiss, title = "BLOCKED NUMBERS", titleColor = NeobrutalOrange) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BLOCK UNKNOWN NUMBERS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = blockUnknown, onCheckedChange = { viewModel.setBlockUnknown(it) })
            }

            HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)

            Text("+ ADD NUMBER TO BLOCK", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NeobrutalTextField(
                    value = newNumber,
                    onValueChange = { newNumber = it },
                    placeholder = "PHONE NUMBER...",
                    modifier = Modifier.weight(1f)
                )
                NeobrutalButton(
                    onClick = {
                        if (newNumber.isNotBlank()) {
                            viewModel.addBlockedRule(newNumber, false, "Manual block")
                            newNumber = ""
                        }
                    },
                    text = "BLOCK",
                    containerColor = NeobrutalOrange,
                    shadowOffset = 3.dp
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(blockedRules) { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeobrutalBgLight)
                            .border(BorderStroke(2.dp, NeobrutalBlack))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rule.pattern, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Unblock",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.deleteBlockedRule(rule) }
                        )
                    }
                }
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 4. Calling Accounts Dialog
@Composable
private fun CallingAccountsDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val choice by viewModel.callingAccountChoice.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "CALLING ACCOUNTS", titleColor = NeobrutalCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MAKE CALLS WITH:", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
            listOf("Ask every time", "SIM 1 (Carrier)", "SIM 2 (Work)").forEach { opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setCallingAccountChoice(opt) }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(selected = choice == opt, onClick = { viewModel.setCallingAccountChoice(opt) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(opt, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                }
            }

            HorizontalDivider(thickness = 2.dp, color = NeobrutalBlack)
            Text("SIP ACCOUNTS", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
            Text("Native VoIP / SIP accounts active.", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 5. Display Options Dialog
@Composable
private fun DisplayOptionsDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val sortOrder by viewModel.sortOrder.collectAsState()
    val nameFormat by viewModel.nameFormat.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "DISPLAY OPTIONS", titleColor = NeobrutalYellow) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SORT BY:", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
            listOf("First name", "Surname").forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setSortOrder(s) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(selected = sortOrder == s, onClick = { viewModel.setSortOrder(s) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                }
            }

            Text("NAME FORMAT:", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
            listOf("First name first", "Surname first").forEach { n ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setNameFormat(n) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(selected = nameFormat == n, onClick = { viewModel.setNameFormat(n) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(n, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                }
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 6. Incoming Gesture Dialog
@Composable
private fun IncomingCallGestureDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val flipSilence by viewModel.gestureFlipSilence.collectAsState()
    val pickupReduce by viewModel.gesturePickupReduce.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "INCOMING GESTURES", titleColor = NeobrutalGreen) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FLIP TO SILENCE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = flipSilence, onCheckedChange = { viewModel.setGestureFlipSilence(it) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PICK UP TO REDUCE VOLUME", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = pickupReduce, onCheckedChange = { viewModel.setGesturePickupReduce(it) })
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 7. Quick Responses Dialog
@Composable
private fun QuickResponsesDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val responses by viewModel.quickResponses.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "QUICK RESPONSES", titleColor = NeobrutalYellow) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CANNED REJECTION TEMPLATES:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.DarkGray)
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(responses) { resp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeobrutalBgLight)
                            .border(BorderStroke(2.dp, NeobrutalBlack))
                            .padding(8.dp)
                    ) {
                        Text(resp.text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeobrutalBlack)
                    }
                }
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 8. Sounds and Vibration Dialog
@Composable
private fun SoundsVibrationDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val vibrate by viewModel.vibrateForCalls.collectAsState()
    val dialTones by viewModel.dialpadTones.collectAsState()
    val callEndTone by viewModel.callEndTone.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "SOUNDS & VIBRATION", titleColor = NeobrutalPink) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VIBRATE FOR CALLS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = vibrate, onCheckedChange = { viewModel.setVibrateForCalls(it) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DIAL PAD TONES", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = dialTones, onCheckedChange = { viewModel.setDialpadTones(it) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CALL END TONE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = callEndTone, onCheckedChange = { viewModel.setCallEndTone(it) })
            }

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 9. Voicemail Dialog
@Composable
private fun VoicemailDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val number by viewModel.voicemailNumber.collectAsState()
    val alerts by viewModel.voicemailAlerts.collectAsState()
    var numInput by remember { mutableStateOf(number) }

    NeobrutalDialog(onDismissRequest = onDismiss, title = "VOICEMAIL SETUP", titleColor = NeobrutalYellow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VOICEMAIL ACCESS NUMBER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, color = NeobrutalBlack)
            NeobrutalTextField(value = numInput, onValueChange = { numInput = it }, placeholder = "*86")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VOICEMAIL ALERTS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = alerts, onCheckedChange = { viewModel.setVoicemailAlerts(it) })
            }

            NeobrutalButton(
                onClick = {
                    viewModel.setVoicemailNumber(numInput)
                    onDismiss()
                },
                text = "SAVE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 10. Contact Ringtones Dialog
@Composable
private fun ContactRingtonesDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val enabled by viewModel.contactRingtones.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "CONTACT RINGTONES", titleColor = NeobrutalCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CUSTOM RINGTONES", color = NeobrutalBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                NeobrutalSwitch(checked = enabled, onCheckedChange = { viewModel.setContactRingtones(it) })
            }
            Text("Ringtones can be bound to contact profiles.", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 11. Calling Card Dialog
@Composable
private fun CallingCardDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val enabled by viewModel.callingCardEnabled.collectAsState()
    val num by viewModel.callingCardNumber.collectAsState()
    val pin by viewModel.callingCardPin.collectAsState()
    var numInput by remember { mutableStateOf(num) }
    var pinInput by remember { mutableStateOf(pin) }

    NeobrutalDialog(onDismissRequest = onDismiss, title = "CALLING CARD", titleColor = NeobrutalOrange) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ENABLE CALLING CARD", color = NeobrutalBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                NeobrutalSwitch(checked = enabled, onCheckedChange = { viewModel.setCallingCardEnabled(it) })
            }

            NeobrutalTextField(value = numInput, onValueChange = { numInput = it }, placeholder = "ACCESS NUMBER...")
            NeobrutalTextField(value = pinInput, onValueChange = { pinInput = it }, placeholder = "PIN CODE...")

            NeobrutalButton(
                onClick = {
                    viewModel.setCallingCardDetails(numInput, pinInput)
                    onDismiss()
                },
                text = "SAVE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 12. Caller ID Announcement Dialog
@Composable
private fun CallerIdAnnouncementDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val current by viewModel.callerIdAnnouncement.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "CALLER ID ANNOUNCEMENT", titleColor = NeobrutalYellow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ANNOUNCE CALLER NAME:", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
            listOf("Always", "Only when using a headset", "Never").forEach { opt ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setCallerIdAnnouncement(opt) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalRadioButton(selected = current == opt, onClick = { viewModel.setCallerIdAnnouncement(opt) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(opt, fontFamily = FontFamily.Monospace, color = NeobrutalBlack)
                }
            }
            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}

// 13. Flip to Silence Dialog
@Composable
private fun FlipToSilenceDialog(viewModel: DialerViewModel, onDismiss: () -> Unit) {
    val enabled by viewModel.flipToSilence.collectAsState()

    NeobrutalDialog(onDismissRequest = onDismiss, title = "FLIP TO SILENCE", titleColor = NeobrutalCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FLIP TO SILENCE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeobrutalBlack)
                NeobrutalSwitch(checked = enabled, onCheckedChange = { viewModel.setFlipToSilence(it) })
            }

            Text(
                text = "To silence an incoming call, place your phone face down on a flat surface.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            NeobrutalButton(
                onClick = onDismiss,
                text = "DONE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            )
        }
    }
}
