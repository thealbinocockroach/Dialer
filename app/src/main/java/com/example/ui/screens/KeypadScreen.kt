package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.t9.T9MatchResult
import com.example.ui.components.NeobrutalBadge
import com.example.ui.components.NeobrutalButton
import com.example.ui.components.NeobrutalCard
import com.example.ui.components.NeobrutalDialog
import com.example.ui.components.NeobrutalIconButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerTab
import com.example.ui.viewmodel.DialerViewModel

data class KeypadKey(
    val digit: String,
    val letters: String
)

private val KEYPAD_KEYS = listOf(
    KeypadKey("1", "VOICEMAIL"),
    KeypadKey("2", "A B C"),
    KeypadKey("3", "D E F"),
    KeypadKey("4", "G H I"),
    KeypadKey("5", "J K L"),
    KeypadKey("6", "M N O"),
    KeypadKey("7", "P Q R S"),
    KeypadKey("8", "T U V"),
    KeypadKey("9", "W X Y Z"),
    KeypadKey("*", ""),
    KeypadKey("0", "+"),
    KeypadKey("#", "")
)

@Composable
fun KeypadScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialedInput by viewModel.dialedInput.collectAsState()
    val t9Results by viewModel.t9SearchResults.collectAsState()
    val haptic = LocalHapticFeedback.current

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (dialedInput.isNotBlank()) {
            viewModel.startCall(dialedInput)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // --- 0. Top Bar: Hide Keypad Action / Tab Navigation ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NeobrutalIconButton(
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                    viewModel.selectTab(DialerTab.RECENTS)
                },
                containerColor = NeobrutalWhite,
                size = 36.dp,
                shadowOffset = 2.dp,
                testTag = "hide_keypad_button",
                icon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hide Keypad",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            if (dialedInput.isNotEmpty()) {
                NeobrutalBadge(
                    text = "${t9Results.size} MATCH${if (t9Results.size == 1) "" else "ES"}",
                    color = if (t9Results.isNotEmpty()) NeobrutalCyan else NeobrutalYellow
                )
            }
        }

        // --- 1. Upper Space (weight = 1f): Expandable T9 Results or Frequent Contacts ---
        // Expands cleanly downward into the available space above the dialpad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            if (dialedInput.isNotEmpty()) {
                if (t9Results.isNotEmpty()) {
                    var expandedMatchId by remember { mutableStateOf<Long?>(null) }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("t9_search_results_list")
                    ) {
                        items(t9Results, key = { "${it.contact.id}_${it.contact.phoneNumber}_${it.contact.displayName}" }) { match ->
                            ContactCardItem(
                                contact = match.contact,
                                isExpanded = expandedMatchId == match.contact.id,
                                onToggleExpand = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    expandedMatchId = if (expandedMatchId == match.contact.id) null else match.contact.id
                                },
                                onCall = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    viewModel.startCall(match.contact.phoneNumber, match.contact.displayName)
                                },
                                onSms = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                    viewModel.swipeActionSms(match.contact.phoneNumber)
                                },
                                onEdit = { /* Not applicable from keypad */ },
                                onCopyNumber = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                    viewModel.copyToClipboard(match.contact.phoneNumber, "Contact Phone Number")
                                },
                                onShare = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                    viewModel.shareContact(match.contact.displayName, match.contact.phoneNumber)
                                },
                                onToggleFavorite = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                    viewModel.toggleFavorite(match.contact)
                                },
                                onDelete = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                    viewModel.deleteContact(match.contact)
                                }
                            )
                        }
                    }
                } else {
                    // Typed number has no matching contacts
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NeobrutalCard(
                            containerColor = NeobrutalWhite,
                            shadowOffset = 2.dp
                        ) {
                            Text(
                                text = "NO MATCHING CONTACTS",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                // When dialedInput is empty, show Frequent Contacts above the keypad so it's convenient & functional
                val frequentContacts by viewModel.frequentlyContacted.collectAsState()
                if (frequentContacts.isNotEmpty()) {
                    var expandedFreqId by remember { mutableStateOf<Long?>(null) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "FREQUENT CONTACTS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(frequentContacts, key = { "${it.lookupKey}_${it.id}_${it.phoneNumber}" }) { contact ->
                                ContactCardItem(
                                    contact = contact,
                                    isExpanded = expandedFreqId == contact.id,
                                    onToggleExpand = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {}
                                        expandedFreqId = if (expandedFreqId == contact.id) null else contact.id
                                    },
                                    onCall = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {}
                                        viewModel.startCall(contact.phoneNumber, contact.displayName)
                                    },
                                    onSms = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        viewModel.swipeActionSms(contact.phoneNumber)
                                    },
                                    onEdit = {},
                                    onCopyNumber = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        viewModel.copyToClipboard(contact.phoneNumber, "Contact Phone Number")
                                    },
                                    onShare = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        viewModel.shareContact(contact.displayName, contact.phoneNumber)
                                    },
                                    onToggleFavorite = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        viewModel.toggleFavorite(contact)
                                    },
                                    onDelete = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        viewModel.deleteContact(contact)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 2. Dialed Number Display (directly above the keypad) ---
        NeobrutalCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeobrutalWhite,
            shadowOffset = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (dialedInput.isNotEmpty()) {
                    Text(
                        text = dialedInput,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = when {
                                dialedInput.length > 15 -> 20.sp
                                dialedInput.length > 11 -> 24.sp
                                dialedInput.length > 8 -> 28.sp
                                else -> 32.sp
                            },
                            color = NeobrutalBlack,
                            letterSpacing = 2.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialed_number_display")
                    )
                } else {
                    Text(
                        text = "ENTER NUMBER OR NAME",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA),
                            letterSpacing = 1.sp
                        ),
                        maxLines = 1
                    )
                }

                if (dialedInput.startsWith("*") || dialedInput.startsWith("#") || dialedInput.contains("#")) {
                    NeobrutalBadge(
                        text = "USSD CODE",
                        color = NeobrutalCyan,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. Keypad Numbers Grid (PLACED AT BOTTOM FOR COMFORTABLE THUMB REACH) ---
        KeypadGrid(
            onDigit = { ch ->
                viewModel.appendDigit(ch)
            },
            onLongZero = {
                viewModel.appendDigit('+')
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- 4. Action Bar (Backspace & Call - AT THE VERY BOTTOM OF SCREEN) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear / Backspace Button
            NeobrutalButton(
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                    viewModel.backspace()
                },
                onLongClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    viewModel.clearInput()
                },
                modifier = Modifier
                    .weight(0.35f)
                    .height(56.dp),
                containerColor = NeobrutalPink,
                contentColor = NeobrutalWhite,
                shadowOffset = 4.dp,
                testTag = "keypad_backspace_button",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = NeobrutalWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            // Primary Call Button
            NeobrutalButton(
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    if (dialedInput.isNotBlank()) {
                        val hasCallPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                        val isUssd = dialedInput.startsWith("*") || dialedInput.startsWith("#") || dialedInput.contains("#")
                        if (!hasCallPermission && !isUssd) {
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        } else {
                            viewModel.startCall(dialedInput)
                        }
                    } else {
                        // Redial last dialed number if available
                        val lastNumber = viewModel.allCallLogs.value.firstOrNull()?.number
                        if (!lastNumber.isNullOrBlank()) {
                            viewModel.setDialedInput(lastNumber)
                        }
                    }
                },
                modifier = Modifier
                    .weight(0.65f)
                    .height(56.dp),
                text = "CALL",
                containerColor = NeobrutalGreen,
                contentColor = NeobrutalBlack,
                shadowOffset = 4.dp,
                testTag = "keypad_call_button",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun KeypadGrid(
    onDigit: (Char) -> Unit,
    onLongZero: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeobrutalCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = NeobrutalWhite,
        shadowOffset = 6.dp,
        borderWidth = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until 3) {
                        val key = KEYPAD_KEYS[row * 3 + col]
                        KeypadButton(
                            key = key,
                            modifier = Modifier.weight(1f),
                            onClick = { onDigit(key.digit[0]) },
                            onLongClick = {
                                if (key.digit == "0") {
                                    onLongZero()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translation = if (isPressed) 2.dp else 0.dp
    val shadowOffset = if (isPressed) 0.dp else 3.dp

    Box(
        modifier = modifier
            .height(58.dp)
            .padding(end = 3.dp, bottom = 3.dp)
            .testTag("keypad_btn_${key.digit}")
    ) {
        // Shadow box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(NeobrutalBlack)
        )

        // Keypad button face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = translation, y = translation)
                .background(NeobrutalWhite)
                .border(BorderStroke(3.dp, NeobrutalBlack))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } catch (e: Exception) { /* Ignore haptic failure */ }
                        onClick()
                    },
                    onLongClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (e: Exception) { /* Ignore haptic failure */ }
                        onLongClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = key.digit,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = NeobrutalBlack
                    )
                )
                if (key.letters.isNotEmpty()) {
                    Text(
                        text = key.letters,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = Color(0xFF555555),
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun T9ContactChip(
    match: T9MatchResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val chipColor = remember(match.contact.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(match.contact.colorHex))
        } catch (_: Exception) {
            NeobrutalYellow
        }
    }

    Box(
        modifier = Modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(NeobrutalBlack)
        )
        Row(
            modifier = Modifier
                .background(chipColor)
                .border(BorderStroke(2.dp, NeobrutalBlack))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = match.contact.displayName.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = NeobrutalBlack
                    )
                )
                Text(
                    text = match.contact.phoneNumber,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NeobrutalBlack.copy(alpha = 0.8f)
                    )
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            NeobrutalBadge(
                text = "CALL",
                color = NeobrutalGreen
            )
        }
    }
}
