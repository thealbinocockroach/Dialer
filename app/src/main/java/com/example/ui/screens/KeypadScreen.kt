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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.t9.T9MatchResult
import com.example.ui.components.NeobrutalBadge
import com.example.ui.components.NeobrutalButton
import com.example.ui.components.NeobrutalCard
import com.example.ui.components.NeobrutalDialog
import com.example.ui.theme.*
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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top Section: Recommendations (T9 matched contacts above the number section) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
        ) {
            if (t9Results.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(t9Results, key = { it.contact.id }) { match ->
                        T9ContactChip(
                            match = match,
                            onClick = {
                                viewModel.startCall(match.contact.phoneNumber, match.contact.displayName)
                            },
                            onLongClick = {
                                viewModel.setDialedInput(match.contact.phoneNumber)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 2. Dialed Number Display (just above the numbers section, empty when blank) ---
        NeobrutalCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeobrutalWhite,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dialedInput, // Completely empty when blank as requested
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = if (dialedInput.length > 12) 22.sp else 28.sp,
                        color = NeobrutalBlack,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialed_number_display")
                )

                if (dialedInput.startsWith("*") || dialedInput.startsWith("#") || dialedInput.contains("#")) {
                    NeobrutalBadge(
                        text = "USSD CODE",
                        color = NeobrutalCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. Floating Keypad Numbers Section ---
        NeobrutalCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeobrutalWhite,
            shadowOffset = 6.dp, // Distinct floating shadow
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
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.appendDigit(key.digit[0])
                                },
                                onLongClick = {
                                    if (key.digit == "0") {
                                        viewModel.appendDigit('+')
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 4. Action Bar (Backspace & Call with perfect Neobrutal shadows) ---
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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.backspace()
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

            // Primary Call Button (Launches in-app calling or USSD without kicking out of app)
            NeobrutalButton(
                onClick = {
                    if (dialedInput.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    onClick = onClick,
                    onLongClick = onLongClick
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
