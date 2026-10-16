package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GroupedCallLog(
    val primaryLog: CallLogEntity,
    val count: Int,
    val logs: List<CallLogEntity>
) {
    val id: Long get() = primaryLog.id
}

@Composable
fun RecentsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val callLogs by viewModel.allCallLogs.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val allBlockedRules by viewModel.allBlockedRules.collectAsState()
    val hasPermission by viewModel.hasCallLogPermission.collectAsState()
    val frequentContacts by viewModel.frequentlyContacted.collectAsState()

    var expandedLogId by remember { mutableStateOf<Long?>(null) }
    var contactToAddNumber by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCallLogPermissionResult(granted)
    }

    var selectedFilter by remember { mutableStateOf<CallType?>(null) } // null = ALL

    val filteredLogs = remember(callLogs, selectedFilter) {
        if (selectedFilter == null) callLogs else callLogs.filter { it.callType == selectedFilter }
    }

    // Group consecutive call logs from the same contact: "name(x)"
    val groupedLogs = remember(filteredLogs) {
        if (filteredLogs.isEmpty()) {
            emptyList<GroupedCallLog>()
        } else {
            val result = mutableListOf<GroupedCallLog>()
            var currentGroup = mutableListOf<CallLogEntity>()

            fun isSameContact(a: CallLogEntity, b: CallLogEntity): Boolean {
                val numA = a.number.filter { it.isDigit() }
                val numB = b.number.filter { it.isDigit() }
                if (numA.isNotBlank() && numB.isNotBlank() && numA == numB) return true
                if (a.number == b.number) return true
                if (a.cachedName.isNotBlank() && a.cachedName != "Unknown" && a.cachedName.equals(b.cachedName, ignoreCase = true)) return true
                return false
            }

            for (log in filteredLogs) {
                if (currentGroup.isEmpty()) {
                    currentGroup.add(log)
                } else {
                    val first = currentGroup.first()
                    if (isSameContact(first, log)) {
                        currentGroup.add(log)
                    } else {
                        result.add(GroupedCallLog(currentGroup.first(), currentGroup.size, currentGroup.toList()))
                        currentGroup = mutableListOf(log)
                    }
                }
            }
            if (currentGroup.isNotEmpty()) {
                result.add(GroupedCallLog(currentGroup.first(), currentGroup.size, currentGroup.toList()))
            }
            result
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
    ) {
        // --- Top Bar (padded on right so floating settings icon never overlaps) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENTS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeobrutalBlack
                )
            )
        }

        // --- Permission Banner if NOT Granted ---
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                NeobrutalCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeobrutalYellow,
                    shadowOffset = 5.dp,
                    borderWidth = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = NeobrutalBlack,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALL HISTORY PERMISSION",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = NeobrutalBlack
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Grant permission to read your device call history so you can view incoming, outgoing, and missed calls.",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = NeobrutalBlack
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NeobrutalButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                            text = "LOAD CALL HISTORY",
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            containerColor = NeobrutalWhite,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 4.dp,
                            testTag = "grant_call_log_permission_button"
                        )
                    }
                }
            }
        } else {
            // --- Frequently Contacted Row (if any) ---
            if (frequentContacts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "FREQUENT CONTACTS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(frequentContacts, key = { "${it.lookupKey}_${it.id}_${it.phoneNumber}" }) { contact ->
                            FrequentlyContactedBlock(
                                contact = contact,
                                onClick = {
                                    viewModel.startCall(contact.phoneNumber, contact.displayName)
                                }
                            )
                        }
                    }
                }
            }

            // --- Filter Chips ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterBadgeChip(
                    title = "ALL",
                    isSelected = selectedFilter == null,
                    count = callLogs.size,
                    onClick = { selectedFilter = null }
                )
                FilterBadgeChip(
                    title = "MISSED",
                    isSelected = selectedFilter == CallType.MISSED,
                    count = callLogs.count { it.callType == CallType.MISSED },
                    accentColor = NeobrutalRed,
                    onClick = { selectedFilter = CallType.MISSED }
                )
                FilterBadgeChip(
                    title = "BLOCKED",
                    isSelected = selectedFilter == CallType.BLOCKED,
                    count = callLogs.count { it.callType == CallType.BLOCKED },
                    accentColor = NeobrutalOrange,
                    onClick = { selectedFilter = CallType.BLOCKED }
                )
            }

            // --- Call Logs Stacked Cards ---
            if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                NeobrutalCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeobrutalWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (hasPermission) "NO CALL LOGS" else "PERMISSION REQUIRED",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeobrutalBlack
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasPermission) "Calls placed or received will appear here." else "Allow call history access above to view call logs.",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupedLogs, key = { "${it.id}_${it.primaryLog.timestamp}_${it.primaryLog.number}" }) { group ->
                    val log = group.primaryLog
                    val cleanLogNumber = remember(log.number) { log.number.filter { it.isDigit() } }
                    val savedContact = remember(allContacts, log.number, cleanLogNumber) {
                        allContacts.firstOrNull {
                            it.phoneNumber == log.number ||
                            (cleanLogNumber.isNotBlank() && it.normalizedNumber == cleanLogNumber)
                        }
                    }
                    val historyLogs = remember(callLogs, log.number, cleanLogNumber) {
                        callLogs.filter {
                            it.number == log.number ||
                            (cleanLogNumber.isNotBlank() && it.number.filter { ch -> ch.isDigit() } == cleanLogNumber)
                        }.take(8)
                    }
                    val isBlocked = remember(allBlockedRules, log.number, cleanLogNumber) {
                        allBlockedRules.any {
                            it.pattern == log.number || (cleanLogNumber.isNotBlank() && it.pattern.filter { ch -> ch.isDigit() } == cleanLogNumber)
                        }
                    }

                    CallLogItemCard(
                        log = log,
                        consecutiveCount = group.count,
                        consecutiveLogs = group.logs,
                        isExpanded = expandedLogId == log.id,
                        savedContact = savedContact,
                        historyLogs = historyLogs,
                        isBlocked = isBlocked,
                        onToggleExpand = {
                            expandedLogId = if (expandedLogId == log.id) null else log.id
                        },
                        onCall = { viewModel.startCall(log.number, log.cachedName) },
                        onSms = { viewModel.swipeActionSms(log.number) },
                        onEditBeforeCall = {
                            viewModel.setDialedInput(log.number)
                            viewModel.selectTab(com.example.ui.viewmodel.DialerTab.KEYPAD)
                        },
                        onRequestAddContact = {
                            contactToAddNumber = log.number
                        },
                        onCopyNumber = {
                            viewModel.copyToClipboard(log.number, "Phone Number")
                        },
                        onToggleBlock = {
                            viewModel.toggleBlockNumber(log.number)
                        },
                        onDelete = { viewModel.deleteCallLogs(group.logs) }
                    )
                }
            }
        }
    }
    }

    if (contactToAddNumber != null) {
        val numberToAdd = contactToAddNumber!!
        var newContactName by remember { mutableStateOf("") }
        var newContactEmail by remember { mutableStateOf("") }
        NeobrutalDialog(
            onDismissRequest = { contactToAddNumber = null },
            title = "ADD TO CONTACTS",
            titleColor = NeobrutalCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PHONE: $numberToAdd",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeobrutalBlack
                    )
                )
                NeobrutalTextField(
                    value = newContactName,
                    onValueChange = { newContactName = it },
                    placeholder = "CONTACT NAME",
                    testTag = "input_contact_name_from_recent"
                )
                NeobrutalTextField(
                    value = newContactEmail,
                    onValueChange = { newContactEmail = it },
                    placeholder = "EMAIL (OPTIONAL)",
                    testTag = "input_contact_email_from_recent"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeobrutalButton(
                        onClick = { contactToAddNumber = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        text = "CANCEL",
                        containerColor = NeobrutalWhite,
                        testTag = "cancel_add_contact_dialog_recent"
                    )
                    NeobrutalButton(
                        onClick = {
                            if (newContactName.isNotBlank()) {
                                viewModel.createContact(newContactName, numberToAdd, newContactEmail)
                                contactToAddNumber = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        text = "SAVE",
                        containerColor = NeobrutalGreen,
                        testTag = "save_contact_dialog_recent"
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequentlyContactedBlock(
    contact: ContactEntity,
    onClick: () -> Unit
) {
    val initials = if (contact.displayName.isNotBlank()) {
        contact.displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    } else "?"

    val bgColor = remember(contact.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(contact.colorHex))
        } catch (_: Exception) {
            NeobrutalYellow
        }
    }

    Box(
        modifier = Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .clickable(onClick = onClick)
            .testTag("frequent_${contact.id}")
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(NeobrutalBlack)
        )
        Column(
            modifier = Modifier
                .width(82.dp)
                .height(86.dp)
                .background(bgColor)
                .border(BorderStroke(3.dp, NeobrutalBlack))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(NeobrutalWhite)
                    .border(BorderStroke(2.dp, NeobrutalBlack)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeobrutalBlack
                    )
                )
            }
            Text(
                text = contact.displayName.take(8).uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = NeobrutalBlack
                ),
                maxLines = 1
            )
            NeobrutalBadge(
                text = "x${contact.callCount}",
                color = NeobrutalWhite
            )
        }
    }
}

@Composable
private fun FilterBadgeChip(
    title: String,
    isSelected: Boolean,
    count: Int,
    accentColor: Color = NeobrutalYellow,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(NeobrutalBlack)
        )
        Row(
            modifier = Modifier
                .background(if (isSelected) accentColor else NeobrutalWhite)
                .border(BorderStroke(2.dp, NeobrutalBlack))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = NeobrutalBlack
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .background(NeobrutalBlack)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = NeobrutalWhite
                    )
                )
            }
        }
    }
}

@Composable
private fun CallLogItemCard(
    log: CallLogEntity,
    consecutiveCount: Int = 1,
    consecutiveLogs: List<CallLogEntity> = listOf(log),
    isExpanded: Boolean,
    savedContact: ContactEntity?,
    historyLogs: List<CallLogEntity>,
    isBlocked: Boolean,
    onToggleExpand: () -> Unit,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onEditBeforeCall: () -> Unit,
    onRequestAddContact: () -> Unit,
    onCopyNumber: () -> Unit,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val initials = if (log.cachedName.isNotBlank() && log.cachedName != "Unknown") {
        log.cachedName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    } else "#"

    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(log.timestamp))
    }

    val (icon, iconColor, typeLabel) = when (log.callType) {
        CallType.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, NeobrutalGreen, "IN")
        CallType.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, NeobrutalBlue, "OUT")
        CallType.MISSED -> Triple(Icons.AutoMirrored.Filled.CallMissed, NeobrutalRed, "MISSED")
        CallType.BLOCKED -> Triple(Icons.Default.Shield, NeobrutalOrange, "BLOCKED")
    }

    NeobrutalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NeobrutalWhite,
        shadowOffset = 3.dp,
        testTag = "call_log_${log.id}",
        onClick = onToggleExpand,
        onLongClick = { showDeleteDialog = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Row (Clicking anywhere here toggles expansion in place)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Initials Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconColor)
                        .border(BorderStroke(2.dp, NeobrutalBlack)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeobrutalBlack
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Middle Info
                Column(modifier = Modifier.weight(1f)) {
                    val rawName = if (log.cachedName.isNotBlank() && log.cachedName != "Unknown") {
                        log.cachedName
                    } else (savedContact?.displayName ?: log.number)
                    val displayName = if (consecutiveCount > 1) "$rawName($consecutiveCount)" else rawName

                    Text(
                        text = displayName.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeobrutalBlack
                        ),
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = typeLabel,
                            tint = NeobrutalBlack,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$typeLabel • $dateStr",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF555555)
                            )
                        )
                        if (log.durationSeconds > 0) {
                            Text(
                                text = "(${log.durationSeconds}s)",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }

                // Expand Chevron Indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand Options",
                    tint = NeobrutalBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Google Phone style in-place expanded actions & history panel
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(NeobrutalBlack)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Primary Action Row: Call, Message, Edit Before Calling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Call
                        NeobrutalButton(
                            onClick = onCall,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            text = "CALL",
                            containerColor = NeobrutalGreen,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 2.dp,
                            testTag = "recent_call_${log.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Quick SMS
                        NeobrutalButton(
                            onClick = onSms,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            text = "SMS",
                            containerColor = NeobrutalBlue,
                            contentColor = NeobrutalWhite,
                            shadowOffset = 2.dp,
                            testTag = "recent_sms_${log.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "SMS",
                                    tint = NeobrutalWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Edit number before call (opens dialpad with number loaded)
                        NeobrutalButton(
                            onClick = onEditBeforeCall,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp),
                            text = "EDIT NUMBER",
                            containerColor = NeobrutalYellow,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 2.dp,
                            testTag = "recent_edit_${log.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Before Call",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Secondary Row: Add Contact (if unsaved) or View Contact (if saved), Copy Number, Block Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (savedContact == null) {
                            // Add Contact button (unsaved number)
                            NeobrutalButton(
                                onClick = onRequestAddContact,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(38.dp),
                                text = "+ CONTACT",
                                containerColor = NeobrutalCyan,
                                contentColor = NeobrutalBlack,
                                shadowOffset = 2.dp,
                                testTag = "recent_add_contact_${log.id}",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add Contact",
                                        tint = NeobrutalBlack,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        } else {
                            // Saved Contact Info badge
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(38.dp)
                                    .background(NeobrutalCyan)
                                    .border(BorderStroke(2.dp, NeobrutalBlack))
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SAVED: ${savedContact.displayName.take(10).uppercase()}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = NeobrutalBlack
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Copy Number
                        NeobrutalButton(
                            onClick = onCopyNumber,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            text = "COPY",
                            containerColor = NeobrutalWhite,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 2.dp,
                            testTag = "recent_copy_${log.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Number",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )

                        // Block / Unblock Number
                        NeobrutalButton(
                            onClick = onToggleBlock,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp),
                            text = if (isBlocked) "UNBLOCK" else "BLOCK",
                            containerColor = if (isBlocked) NeobrutalOrange else NeobrutalRed,
                            contentColor = NeobrutalWhite,
                            shadowOffset = 2.dp,
                            testTag = "recent_block_${log.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Block",
                                    tint = NeobrutalWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Call History with this Number (in the same place, like Google Phone app)
                    Text(
                        text = "CALL HISTORY (${historyLogs.size})",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F7F7))
                            .border(BorderStroke(1.dp, Color(0xFFDDDDDD)))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        historyLogs.take(5).forEach { history ->
                            val historyDate = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(history.timestamp))
                            val (hIcon, _, hLabel) = when (history.callType) {
                                CallType.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, NeobrutalGreen, "Incoming")
                                CallType.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, NeobrutalBlue, "Outgoing")
                                CallType.MISSED -> Triple(Icons.AutoMirrored.Filled.CallMissed, NeobrutalRed, "Missed")
                                CallType.BLOCKED -> Triple(Icons.Default.Shield, NeobrutalOrange, "Blocked")
                            }
                            val durationText = if (history.durationSeconds > 0) {
                                val mins = history.durationSeconds / 60
                                val secs = history.durationSeconds % 60
                                if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                            } else if (history.callType == CallType.MISSED) {
                                "Not answered"
                            } else {
                                "0s"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = hIcon,
                                        contentDescription = hLabel,
                                        tint = NeobrutalBlack,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$hLabel • $historyDate",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = NeobrutalBlack
                                        )
                                    )
                                }
                                Text(
                                    text = durationText,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val displayName = if (log.cachedName.isNotBlank()) log.cachedName else log.number
        NeobrutalDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "DELETE CALL LOG",
            titleColor = NeobrutalRed
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Delete call history with ${displayName.uppercase()}?",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeobrutalBlack
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeobrutalButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f).height(44.dp),
                        text = "CANCEL",
                        containerColor = NeobrutalWhite,
                        testTag = "cancel_delete_log_btn"
                    )
                    NeobrutalButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        text = "DELETE",
                        containerColor = NeobrutalRed,
                        testTag = "confirm_delete_log_btn"
                    )
                }
            }
        }
    }
}
