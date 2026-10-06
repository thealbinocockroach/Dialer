package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallState
import com.example.data.model.OverlayMode
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import java.util.Locale

@Composable
fun ActiveCallContainer(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val activeCall by viewModel.activeCall.collectAsState()
    val quickResponses by viewModel.quickResponses.collectAsState()
    var showQuickDeclineSheet by remember { mutableStateOf(false) }
    var showInCallKeypad by remember { mutableStateOf(false) }

    if (activeCall == null) return

    val session = activeCall!!

    when (session.overlayMode) {
        OverlayMode.FULL_SCREEN -> {
            FullScreenCallView(
                session = session,
                onAnswer = { viewModel.answerCall() },
                onEndCall = { viewModel.endCall() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onToggleHold = { viewModel.toggleHold() },
                onToggleRecord = { viewModel.toggleCallRecording() },
                onSwitchMode = { mode -> viewModel.setOverlayMode(mode) },
                onOpenQuickDecline = { showQuickDeclineSheet = true },
                onOpenKeypad = { showInCallKeypad = !showInCallKeypad }
            )
        }
        OverlayMode.POP_UP -> {
            PopUpCallHud(
                session = session,
                onAnswer = { viewModel.answerCall() },
                onDecline = { viewModel.declineCall() },
                onEndCall = { viewModel.endCall() },
                onExpand = { viewModel.setOverlayMode(OverlayMode.FULL_SCREEN) },
                onMini = { viewModel.setOverlayMode(OverlayMode.MINI_POP_UP) },
                onOpenQuickDecline = { showQuickDeclineSheet = true }
            )
        }
        OverlayMode.MINI_POP_UP -> {
            MiniPopUpCallHud(
                session = session,
                onEndCall = { viewModel.endCall() },
                onExpand = { viewModel.setOverlayMode(OverlayMode.FULL_SCREEN) }
            )
        }
    }

    // Quick Decline Bottom Sheet
    if (showQuickDeclineSheet) {
        QuickDeclineDrawer(
            responses = quickResponses.map { it.text },
            onDismiss = { showQuickDeclineSheet = false },
            onSelect = { text ->
                showQuickDeclineSheet = false
                viewModel.quickDeclineCall(text)
            }
        )
    }

    // In-call Keypad Dialog
    if (showInCallKeypad) {
        InCallKeypadDialog(
            onDismiss = { showInCallKeypad = false },
            onDigit = { digit -> viewModel.appendDigit(digit) }
        )
    }
}

// --- 1. FULL SCREEN CALL VIEW ---
@Composable
private fun FullScreenCallView(
    session: ActiveCallSession,
    onAnswer: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleRecord: () -> Unit,
    onSwitchMode: (OverlayMode) -> Unit,
    onOpenQuickDecline: () -> Unit,
    onOpenKeypad: () -> Unit
) {
    val durationFormatted = remember(session.callDurationSeconds) {
        val mins = session.callDurationSeconds / 60
        val secs = session.callDurationSeconds % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    val recordDurationFormatted = remember(session.recordingDurationSeconds) {
        val mins = session.recordingDurationSeconds / 60
        val secs = session.recordingDurationSeconds % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    // Flashing animation for active recording
    val infiniteTransition = rememberInfiniteTransition(label = "rec_flash")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .testTag("full_screen_call_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Mode switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeobrutalBadge(
                    text = if (session.isIncoming) "INCOMING CALL" else "ACTIVE CALL",
                    color = if (session.isIncoming) NeobrutalPink else NeobrutalYellow
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeobrutalButton(
                        onClick = { onSwitchMode(OverlayMode.POP_UP) },
                        text = "POP-UP",
                        containerColor = NeobrutalYellow,
                        borderWidth = 2.dp,
                        shadowOffset = 2.dp,
                        testTag = "btn_switch_popup"
                    )
                    NeobrutalButton(
                        onClick = { onSwitchMode(OverlayMode.MINI_POP_UP) },
                        text = "MINI",
                        containerColor = NeobrutalCyan,
                        borderWidth = 2.dp,
                        shadowOffset = 2.dp,
                        testTag = "btn_switch_mini"
                    )
                }
            }

            // Caller Identity Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Avatar Initials Block
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(NeobrutalYellow)
                        .border(BorderStroke(4.dp, NeobrutalBlack)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.contactInitials,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            color = NeobrutalBlack
                        )
                    )
                }

                // Caller Name
                Text(
                    text = session.callerName.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = NeobrutalBlack,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1
                )

                // Caller Number
                Text(
                    text = session.number,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF444444)
                    )
                )

                // Call Timer Card
                NeobrutalCard(
                    modifier = Modifier.width(180.dp),
                    containerColor = NeobrutalWhite,
                    shadowOffset = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (session.callState == CallState.RINGING) "RINGING..." else durationFormatted,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = if (session.callState == CallState.RINGING) NeobrutalPink else NeobrutalBlack
                            )
                        )
                    }
                }

                // Recording Status Indicator
                if (session.isRecording) {
                    Box(
                        modifier = Modifier
                            .alpha(recAlpha)
                            .background(NeobrutalRed)
                            .border(BorderStroke(2.dp, NeobrutalBlack))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeobrutalWhite)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REC ACTIVE • $recordDurationFormatted",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = NeobrutalWhite
                                )
                            )
                        }
                    }
                }
            }

            // In-Call Controls Matrix or Answer/Decline Actions
            if (session.callState == CallState.RINGING) {
                // Incoming Call Action Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        NeobrutalButton(
                            onClick = onEndCall,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            text = "DECLINE",
                            containerColor = NeobrutalRed,
                            contentColor = NeobrutalWhite,
                            testTag = "incoming_decline_btn",
                            icon = {
                                Icon(
                                    Icons.Default.CallEnd,
                                    contentDescription = "Decline",
                                    tint = NeobrutalWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )

                        NeobrutalButton(
                            onClick = onAnswer,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            text = "ANSWER",
                            containerColor = NeobrutalGreen,
                            contentColor = NeobrutalBlack,
                            testTag = "incoming_answer_btn",
                            icon = {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Answer",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }

                    // Quick Decline SMS Trigger
                    NeobrutalButton(
                        onClick = onOpenQuickDecline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        text = "QUICK DECLINE WITH SMS",
                        containerColor = NeobrutalWhite,
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp,
                        testTag = "incoming_quick_decline_btn",
                        icon = {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = "SMS",
                                tint = NeobrutalBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            } else {
                // Active In-Call Control Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Controls Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InCallControlButton(
                            text = if (session.isMuted) "UNMUTE" else "MUTE",
                            icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            isActive = session.isMuted,
                            onClick = onToggleMute,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_incall_mute"
                        )
                        InCallControlButton(
                            text = "KEYPAD",
                            icon = Icons.Default.Dialpad,
                            isActive = false,
                            onClick = onOpenKeypad,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_incall_keypad"
                        )
                        InCallControlButton(
                            text = if (session.isSpeakerOn) "EARPIECE" else "SPEAKER",
                            icon = if (session.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            isActive = session.isSpeakerOn,
                            onClick = onToggleSpeaker,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_incall_speaker"
                        )
                    }

                    // Controls Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InCallControlButton(
                            text = if (session.isHold) "RESUME" else "HOLD",
                            icon = Icons.Default.Pause,
                            isActive = session.isHold,
                            onClick = onToggleHold,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_incall_hold"
                        )
                        InCallControlButton(
                            text = if (session.isRecording) "STOP REC" else "RECORD",
                            icon = Icons.Default.FiberManualRecord,
                            isActive = session.isRecording,
                            activeColor = NeobrutalRed,
                            onClick = onToggleRecord,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_incall_record"
                        )
                        InCallControlButton(
                            text = "QUICK MSG",
                            icon = Icons.Default.Message,
                            isActive = false,
                            onClick = onOpenQuickDecline,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_incall_quick_msg"
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big End Call Button
                    NeobrutalButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        text = "END CALL",
                        containerColor = NeobrutalRed,
                        contentColor = NeobrutalWhite,
                        testTag = "incall_end_call_button",
                        icon = {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = NeobrutalWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

// --- 2. POP-UP CALL CARD ---
@Composable
private fun PopUpCallHud(
    session: ActiveCallSession,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: () -> Unit,
    onExpand: () -> Unit,
    onMini: () -> Unit,
    onOpenQuickDecline: () -> Unit
) {
    val durationFormatted = remember(session.callDurationSeconds) {
        val mins = session.callDurationSeconds / 60
        val secs = session.callDurationSeconds % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("popup_call_hud")
    ) {
        NeobrutalCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeobrutalYellow,
            shadowOffset = 5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalBadge(
                        text = if (session.callState == CallState.RINGING) "INCOMING CALL" else "CALL IN PROGRESS",
                        color = NeobrutalWhite
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NeobrutalButton(
                            onClick = onMini,
                            text = "MINI",
                            containerColor = NeobrutalCyan,
                            borderWidth = 2.dp,
                            shadowOffset = 2.dp
                        )
                        NeobrutalButton(
                            onClick = onExpand,
                            text = "EXPAND",
                            containerColor = NeobrutalWhite,
                            borderWidth = 2.dp,
                            shadowOffset = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Caller Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(NeobrutalWhite)
                            .border(BorderStroke(3.dp, NeobrutalBlack)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = session.contactInitials,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = NeobrutalBlack
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.callerName.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeobrutalBlack
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = session.number,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        )
                        Text(
                            text = if (session.callState == CallState.RINGING) "RINGING..." else durationFormatted,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = NeobrutalBlack
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                if (session.callState == CallState.RINGING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeobrutalButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            text = "DECLINE",
                            containerColor = NeobrutalRed,
                            contentColor = NeobrutalWhite,
                            testTag = "popup_decline_btn"
                        )
                        NeobrutalButton(
                            onClick = onAnswer,
                            modifier = Modifier.weight(1f),
                            text = "ANSWER",
                            containerColor = NeobrutalGreen,
                            contentColor = NeobrutalBlack,
                            testTag = "popup_answer_btn"
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeobrutalButton(
                            onClick = onOpenQuickDecline,
                            modifier = Modifier.weight(1f),
                            text = "SEND SMS",
                            containerColor = NeobrutalBlue,
                            contentColor = NeobrutalWhite
                        )
                        NeobrutalButton(
                            onClick = onEndCall,
                            modifier = Modifier.weight(1f),
                            text = "END CALL",
                            containerColor = NeobrutalRed,
                            contentColor = NeobrutalWhite,
                            testTag = "popup_end_call_btn"
                        )
                    }
                }
            }
        }
    }
}

// --- 3. MINI POP-UP CALL CARD ---
@Composable
private fun MiniPopUpCallHud(
    session: ActiveCallSession,
    onEndCall: () -> Unit,
    onExpand: () -> Unit
) {
    val durationFormatted = remember(session.callDurationSeconds) {
        val mins = session.callDurationSeconds / 60
        val secs = session.callDurationSeconds % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("mini_popup_call_hud")
    ) {
        NeobrutalCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeobrutalWhite,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left avatar & caller info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onExpand() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(NeobrutalYellow)
                            .border(BorderStroke(2.dp, NeobrutalBlack)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = session.contactInitials,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = NeobrutalBlack
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = session.callerName.take(12).uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = NeobrutalBlack
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = if (session.callState == CallState.RINGING) "RINGING..." else durationFormatted,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        )
                    }
                }

                // Tiny action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeobrutalIconButton(
                        onClick = onExpand,
                        containerColor = NeobrutalYellow,
                        size = 36.dp,
                        shadowOffset = 2.dp,
                        icon = {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Expand",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    NeobrutalIconButton(
                        onClick = onEndCall,
                        containerColor = NeobrutalRed,
                        contentColor = NeobrutalWhite,
                        size = 36.dp,
                        shadowOffset = 2.dp,
                        testTag = "mini_end_call_btn",
                        icon = {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = NeobrutalWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

// --- Quick Decline Bottom Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDeclineDrawer(
    responses: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeobrutalWhite,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(4.dp, NeobrutalBlack))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK DECLINE WITH SMS",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeobrutalBlack
                    )
                )
                Text(
                    text = "[X]",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeobrutalBlack
                    ),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(responses) { text ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 3.dp, bottom = 3.dp)
                            .clickable { onSelect(text) }
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(NeobrutalBlack)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeobrutalBgLight)
                                .border(BorderStroke(2.dp, NeobrutalBlack))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = text,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NeobrutalBlack
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// In-call Keypad Dialog for DTMF digits
@Composable
private fun InCallKeypadDialog(
    onDismiss: () -> Unit,
    onDigit: (Char) -> Unit
) {
    NeobrutalDialog(
        onDismissRequest = onDismiss,
        title = "IN-CALL DTMF KEYPAD",
        titleColor = NeobrutalYellow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { digit ->
                        NeobrutalButton(
                            onClick = { onDigit(digit[0]) },
                            modifier = Modifier.weight(1f),
                            text = digit,
                            containerColor = NeobrutalWhite,
                            borderWidth = 2.dp,
                            shadowOffset = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = NeobrutalYellow,
    testTag: String? = null
) {
    val bgColor = if (isActive) activeColor else NeobrutalWhite

    Box(
        modifier = modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(NeobrutalBlack)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .border(BorderStroke(2.dp, NeobrutalBlack))
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = NeobrutalBlack,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    color = NeobrutalBlack
                ),
                maxLines = 1
            )
        }
    }
}
