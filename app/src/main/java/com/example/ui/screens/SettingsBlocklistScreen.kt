package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutoRecordRule
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.QuickResponseEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun SettingsBlocklistScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val autoAnswerEnabled by viewModel.autoAnswerEnabled.collectAsState()
    val autoAnswerDelay by viewModel.autoAnswerDelay.collectAsState()
    val autoRecordRule by viewModel.autoRecordRule.collectAsState()
    val blockUnknownNumbers by viewModel.blockUnknownNumbers.collectAsState()
    val blockedRules by viewModel.allBlockedRules.collectAsState()
    val quickResponses by viewModel.quickResponses.collectAsState()

    var showAddBlockDialog by remember { mutableStateOf(false) }
    var showAddQuickResponseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header ---
        item {
            Text(
                text = "SETTINGS & BLOCK RULES",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeobrutalBlack
                )
            )
        }

        // --- 1. Call Blocking Engine Card ---
        item {
            NeobrutalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeobrutalWhite,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALL BLOCKING ENGINE",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeobrutalBlack
                            )
                        )
                        NeobrutalBadge(
                            text = "SHIELD ACTIVE",
                            color = NeobrutalOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Block Unknown Numbers Toggle
                    NeobrutalToggleRow(
                        label = "BLOCK PRIVATE / UNKNOWN NUMBERS",
                        subtitle = "Immediately reject incoming calls without caller ID",
                        isChecked = blockUnknownNumbers,
                        onCheckedChange = { viewModel.setBlockUnknown(it) },
                        testTag = "toggle_block_unknown"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Blocked Rules Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BLOCKED NUMBERS & PREFIXES (${blockedRules.size})",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        )

                        NeobrutalButton(
                            onClick = { showAddBlockDialog = true },
                            text = "+ ADD RULE",
                            containerColor = NeobrutalYellow,
                            borderWidth = 2.dp,
                            shadowOffset = 2.dp,
                            testTag = "add_block_rule_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Blocked list items
                    blockedRules.forEach { rule ->
                        BlockedRuleRow(
                            rule = rule,
                            onDelete = { viewModel.deleteBlockedRule(rule) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // --- 2. Auto-Record Rules Card ---
        item {
            NeobrutalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeobrutalWhite,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALL RECORDING RULES",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeobrutalBlack
                            )
                        )
                        NeobrutalBadge(
                            text = autoRecordRule.name,
                            color = NeobrutalPink,
                            textColor = NeobrutalWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose which calls are automatically recorded locally:",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AutoRecordRule.values().forEach { rule ->
                        RuleOptionRow(
                            title = when (rule) {
                                AutoRecordRule.ALL -> "RECORD ALL CALLS"
                                AutoRecordRule.UNSAVED -> "RECORD UNSAVED NUMBERS ONLY"
                                AutoRecordRule.SPECIFIC_CONTACTS -> "RECORD SPECIFIC CONTACT LIST"
                                AutoRecordRule.OFF -> "DISABLED (MANUAL ONLY)"
                            },
                            isSelected = autoRecordRule == rule,
                            onClick = { viewModel.setAutoRecord(rule) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // --- 3. Auto-Answer Controller Card ---
        item {
            NeobrutalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeobrutalWhite,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTO-ANSWER CONTROLLER",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeobrutalBlack
                            )
                        )
                        NeobrutalBadge(
                            text = if (autoAnswerEnabled) "ENABLED" else "OFF",
                            color = if (autoAnswerEnabled) NeobrutalGreen else NeobrutalGrayLight
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    NeobrutalToggleRow(
                        label = "ENABLE AUTO-ANSWER",
                        subtitle = "Automatically accept incoming calls after delay",
                        isChecked = autoAnswerEnabled,
                        onCheckedChange = { viewModel.setAutoAnswer(it) },
                        testTag = "toggle_auto_answer"
                    )

                    if (autoAnswerEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DELAY BEFORE ANSWERING: ${autoAnswerDelay} SECONDS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeobrutalBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 3, 5, 10).forEach { sec ->
                                Box(
                                    modifier = Modifier
                                        .background(if (autoAnswerDelay == sec) NeobrutalYellow else NeobrutalWhite)
                                        .border(BorderStroke(2.dp, NeobrutalBlack))
                                        .clickable { viewModel.setAutoAnswerDelaySeconds(sec) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = NeobrutalBlack
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Quick Decline Responses ---
        item {
            NeobrutalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeobrutalWhite,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK DECLINE RESPONDER",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeobrutalBlack
                            )
                        )
                        NeobrutalButton(
                            onClick = { showAddQuickResponseDialog = true },
                            text = "+ ADD",
                            containerColor = NeobrutalYellow,
                            borderWidth = 2.dp,
                            shadowOffset = 2.dp,
                            testTag = "add_quick_response_btn"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pre-formatted SMS messages sent when declining incoming calls:",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    quickResponses.forEach { qr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeobrutalBgLight)
                                .border(BorderStroke(2.dp, NeobrutalBlack))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = qr.text,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NeobrutalBlack
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.deleteQuickResponse(qr) }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddBlockDialog) {
        AddBlockRuleDialog(
            onDismiss = { showAddBlockDialog = false },
            onConfirm = { pattern, isPrefix, note ->
                viewModel.addBlockedRule(pattern, isPrefix, note)
                showAddBlockDialog = false
            }
        )
    }

    if (showAddQuickResponseDialog) {
        AddQuickResponseDialog(
            onDismiss = { showAddQuickResponseDialog = false },
            onConfirm = { text ->
                viewModel.addQuickResponse(text)
                showAddQuickResponseDialog = false
            }
        )
    }
}

@Composable
private fun NeobrutalToggleRow(
    label: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = NeobrutalBlack
                )
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (isChecked) NeobrutalGreen else NeobrutalWhite)
                .border(BorderStroke(2.dp, NeobrutalBlack))
                .clickable { onCheckedChange(!isChecked) }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Text(
                    text = "V",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeobrutalBlack
                    )
                )
            }
        }
    }
}

@Composable
private fun BlockedRuleRow(
    rule: BlockedNumberEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeobrutalBgLight)
            .border(BorderStroke(2.dp, NeobrutalBlack))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (rule.isPrefixPattern) "PREFIX: ${rule.pattern}*" else rule.pattern,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = NeobrutalBlack
                    )
                )
                if (rule.blockedCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    NeobrutalBadge(
                        text = "BLOCKED x${rule.blockedCount}",
                        color = NeobrutalOrange
                    )
                }
            }
            if (rule.note.isNotBlank()) {
                Text(
                    text = rule.note,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp,
                        color = Color.DarkGray
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = Color.Gray,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun RuleOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) NeobrutalYellow.copy(alpha = 0.3f) else NeobrutalWhite)
            .border(BorderStroke(2.dp, if (isSelected) NeobrutalBlack else Color.LightGray))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (isSelected) NeobrutalBlack else NeobrutalWhite)
                .border(BorderStroke(2.dp, NeobrutalBlack))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = 11.sp,
                color = NeobrutalBlack
            )
        )
    }
}

@Composable
private fun AddBlockRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, isPrefix: Boolean, note: String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var isPrefix by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    NeobrutalDialog(
        onDismissRequest = onDismiss,
        title = "ADD BLOCK RULE",
        titleColor = NeobrutalOrange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NeobrutalTextField(
                value = pattern,
                onValueChange = { pattern = it },
                placeholder = "NUMBER OR PREFIX (E.G. 800)",
                testTag = "input_block_pattern"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPrefix = !isPrefix },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(if (isPrefix) NeobrutalOrange else NeobrutalWhite)
                        .border(BorderStroke(2.dp, NeobrutalBlack)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPrefix) {
                        Text(
                            text = "V",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = NeobrutalBlack
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MATCH AS AREA CODE / PREFIX",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeobrutalBlack
                    )
                )
            }

            NeobrutalTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "REASON / NOTE (E.G. SPAM TELEMARKETER)",
                testTag = "input_block_note"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeobrutalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    text = "CANCEL",
                    containerColor = NeobrutalWhite
                )

                NeobrutalButton(
                    onClick = {
                        if (pattern.isNotBlank()) {
                            onConfirm(pattern, isPrefix, note)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = "BLOCK",
                    containerColor = NeobrutalOrange,
                    enabled = pattern.isNotBlank(),
                    testTag = "confirm_block_rule_button"
                )
            }
        }
    }
}

@Composable
private fun AddQuickResponseDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    NeobrutalDialog(
        onDismissRequest = onDismiss,
        title = "NEW QUICK RESPONSE",
        titleColor = NeobrutalYellow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NeobrutalTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "ENTER MESSAGE TEMPLATE...",
                testTag = "input_quick_response_text"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeobrutalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    text = "CANCEL",
                    containerColor = NeobrutalWhite
                )

                NeobrutalButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onConfirm(text)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = "SAVE",
                    containerColor = NeobrutalGreen,
                    enabled = text.isNotBlank(),
                    testTag = "save_quick_response_btn"
                )
            }
        }
    }
}
