package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.ui.components.NeobrutalBadge
import com.example.ui.components.NeobrutalCard
import com.example.ui.components.NeobrutalIconButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecentsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val callLogs by viewModel.allCallLogs.collectAsState()
    val frequentContacts by viewModel.frequentlyContacted.collectAsState()

    var selectedFilter by remember { mutableStateOf<CallType?>(null) } // null = ALL

    val filteredLogs = remember(callLogs, selectedFilter) {
        if (selectedFilter == null) callLogs else callLogs.filter { it.callType == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
    ) {
        // --- Top Bar with Clear Action ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RECENTS & LOGS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeobrutalBlack
                )
            )

            if (callLogs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clickable { viewModel.clearAllLogs() }
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .background(NeobrutalWhite)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CLEAR ALL",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = NeobrutalBlack
                        )
                    )
                }
            }
        }

        // --- Frequently Contacted Row ---
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
                    items(frequentContacts, key = { it.id }) { contact ->
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
            FilterBadgeChip(
                title = "RECORDED",
                isSelected = false,
                count = callLogs.count { it.isRecorded },
                accentColor = NeobrutalPink,
                onClick = { viewModel.selectTab(com.example.ui.viewmodel.DialerTab.RECORDINGS) }
            )
        }

        // --- Swipe Instruction Note ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SWIPE RIGHT -> CALL",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF008800)
                )
            )
            Text(
                text = "SWIPE LEFT -> SMS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = NeobrutalBlue
                )
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
                            text = "NO CALL LOGS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeobrutalBlack
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calls made or received will appear here.",
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
                items(filteredLogs, key = { it.id }) { log ->
                    SwipeableCallLogCard(
                        log = log,
                        onCall = { viewModel.swipeActionCall(log.number, log.cachedName) },
                        onSms = { viewModel.swipeActionSms(log.number) },
                        onDelete = { viewModel.deleteCallLog(log) }
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

    val bgColor = Color(android.graphics.Color.parseColor(contact.colorHex))

    Box(
        modifier = Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .clickable(onClick = onClick)
            .testTag("frequent_${contact.id}")
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(NeobrutalBlack)
        )
        Column(
            modifier = Modifier
                .width(82.dp)
                .height(90.dp)
                .background(bgColor)
                .border(BorderStroke(3.dp, NeobrutalBlack))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NeobrutalWhite)
                    .border(BorderStroke(2.dp, NeobrutalBlack)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
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
                .offset(x = 3.dp, y = 3.dp)
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
private fun SwipeableCallLogCard(
    log: CallLogEntity,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onDelete: () -> Unit
) {
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

    // Direct swipe action container or action card
    NeobrutalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NeobrutalWhite,
        shadowOffset = 4.dp,
        testTag = "call_log_${log.id}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Initials Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconColor)
                    .border(BorderStroke(2.dp, NeobrutalBlack)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = NeobrutalBlack
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Middle Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (log.cachedName.isNotBlank()) log.cachedName else log.number).uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeobrutalBlack
                        ),
                        maxLines = 1
                    )
                }

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

                if (log.isRecorded) {
                    Spacer(modifier = Modifier.height(2.dp))
                    NeobrutalBadge(
                        text = "REC AUDIO",
                        color = NeobrutalPink,
                        textColor = NeobrutalWhite
                    )
                }
            }

            // Right Quick Actions (Call, SMS, Delete)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Call Action (Swipe right alternative)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeobrutalGreen)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable { onCall() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // SMS Action (Swipe left alternative)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeobrutalBlue)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable { onSms() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "SMS",
                        tint = NeobrutalWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Action
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeobrutalWhite)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
