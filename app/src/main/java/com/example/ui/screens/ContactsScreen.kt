package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun ContactsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.allContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var speedDialTargetContact by remember { mutableStateOf<ContactEntity?>(null) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts else {
            contacts.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .padding(14.dp)
    ) {
        // --- Header & Add Button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONTACTS (${contacts.size})",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeobrutalBlack
                )
            )

            NeobrutalButton(
                onClick = { showAddDialog = true },
                text = "+ ADD",
                containerColor = NeobrutalYellow,
                testTag = "add_contact_button"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Search Bar ---
        NeobrutalTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "SEARCH CONTACTS...",
            testTag = "contact_search_field",
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = NeobrutalBlack,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { searchQuery = "" }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- Contacts List ---
        if (filteredContacts.isEmpty()) {
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
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO CONTACTS FOUND",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = NeobrutalBlack
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.id }) { contact ->
                    ContactCardItem(
                        contact = contact,
                        onCall = { viewModel.startCall(contact.phoneNumber, contact.displayName) },
                        onSms = { viewModel.swipeActionSms(contact.phoneNumber) },
                        onSpeedDial = { speedDialTargetContact = contact },
                        onDelete = { viewModel.deleteContact(contact) }
                    )
                }
            }
        }
    }

    // --- Add Contact Dialog ---
    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, number, email, speedDial ->
                viewModel.createContact(name, number, email, speedDial)
                showAddDialog = false
            }
        )
    }

    // --- Speed Dial Assignment Dialog ---
    if (speedDialTargetContact != null) {
        val contact = speedDialTargetContact!!
        SpeedDialAssignDialog(
            contactName = contact.displayName,
            currentDigit = contact.speedDialDigit,
            onDismiss = { speedDialTargetContact = null },
            onAssign = { digit ->
                viewModel.assignSpeedDial(contact.id, digit)
                speedDialTargetContact = null
            },
            onClear = {
                if (contact.speedDialDigit != null) {
                    viewModel.clearSpeedDial(contact.speedDialDigit)
                }
                speedDialTargetContact = null
            }
        )
    }
}

@Composable
private fun ContactCardItem(
    contact: ContactEntity,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onSpeedDial: () -> Unit,
    onDelete: () -> Unit
) {
    val initials = if (contact.displayName.isNotBlank()) {
        contact.displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    } else "#"

    val avatarColor = Color(android.graphics.Color.parseColor(contact.colorHex))

    NeobrutalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NeobrutalWhite,
        shadowOffset = 3.dp,
        testTag = "contact_item_${contact.id}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(avatarColor)
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

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeobrutalBlack
                        ),
                        maxLines = 1
                    )
                    if (contact.speedDialDigit != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NeobrutalBadge(
                            text = "SPEED #${contact.speedDialDigit}",
                            color = NeobrutalYellow
                        )
                    }
                }

                Text(
                    text = contact.phoneNumber,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF444444)
                    )
                )

                if (contact.email.isNotBlank()) {
                    Text(
                        text = contact.email,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 9.sp,
                            color = Color.Gray
                        ),
                        maxLines = 1
                    )
                }
            }

            // Quick Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Speed Dial Assign
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(NeobrutalYellow)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable(onClick = onSpeedDial),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Speed Dial",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Call
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(NeobrutalGreen)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable(onClick = onCall),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // SMS
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(NeobrutalBlue)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable(onClick = onSms),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "SMS",
                        tint = NeobrutalWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(NeobrutalWhite)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String, email: String, speedDial: Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var speedDialStr by remember { mutableStateOf("") }

    NeobrutalDialog(
        onDismissRequest = onDismiss,
        title = "NEW CONTACT",
        titleColor = NeobrutalYellow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NeobrutalTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "FULL NAME",
                testTag = "input_contact_name"
            )

            NeobrutalTextField(
                value = number,
                onValueChange = { number = it },
                placeholder = "PHONE NUMBER (+1...)",
                testTag = "input_contact_number"
            )

            NeobrutalTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "EMAIL ADDRESS (OPTIONAL)",
                testTag = "input_contact_email"
            )

            NeobrutalTextField(
                value = speedDialStr,
                onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 2) speedDialStr = it },
                placeholder = "SPEED DIAL DIGIT (2-99)",
                testTag = "input_contact_speed_dial"
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
                        if (name.isNotBlank() && number.isNotBlank()) {
                            val digit = speedDialStr.toIntOrNull()
                            onConfirm(name, number, email, digit)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = "SAVE",
                    containerColor = NeobrutalGreen,
                    enabled = name.isNotBlank() && number.isNotBlank(),
                    testTag = "save_contact_button"
                )
            }
        }
    }
}

@Composable
private fun SpeedDialAssignDialog(
    contactName: String,
    currentDigit: Int?,
    onDismiss: () -> Unit,
    onAssign: (Int) -> Unit,
    onClear: () -> Unit
) {
    var digitInput by remember { mutableStateOf(currentDigit?.toString() ?: "") }

    NeobrutalDialog(
        onDismissRequest = onDismiss,
        title = "SPEED DIAL: $contactName",
        titleColor = NeobrutalYellow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Assign key (2 - 99) for fast long-press dialing.",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeobrutalBlack
                )
            )

            NeobrutalTextField(
                value = digitInput,
                onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 2) digitInput = it },
                placeholder = "DIGIT (2-99)",
                testTag = "speed_dial_input"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentDigit != null) {
                    NeobrutalButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        text = "CLEAR",
                        containerColor = NeobrutalPink,
                        contentColor = NeobrutalWhite
                    )
                }

                NeobrutalButton(
                    onClick = {
                        val digit = digitInput.toIntOrNull()
                        if (digit != null && digit in 2..99) {
                            onAssign(digit)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = "SET DIAL",
                    containerColor = NeobrutalGreen,
                    enabled = digitInput.toIntOrNull()?.let { it in 2..99 } == true
                )
            }
        }
    }
}
