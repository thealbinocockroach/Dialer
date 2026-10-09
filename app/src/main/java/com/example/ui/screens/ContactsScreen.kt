package com.example.ui.screens

import android.Manifest
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
    val hasPermission by viewModel.hasContactsPermission.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermissionResult(granted)
    }

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

        // --- Permission Banner if NOT Granted ---
        if (!hasPermission) {
            NeobrutalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeobrutalYellow,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = NeobrutalBlack,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEVICE CONTACTS ACCESS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeobrutalBlack
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Allow access to read your phone's address book so your real contacts appear in Brutal Dial.",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = NeobrutalBlack
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NeobrutalButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        text = "GRANT CONTACTS PERMISSION",
                        containerColor = NeobrutalWhite,
                        testTag = "grant_contacts_permission_btn"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

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
                            text = if (hasPermission) "NO CONTACTS FOUND" else "PERMISSION REQUIRED",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = NeobrutalBlack
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasPermission) "Tap + ADD above or sync device address book." else "Grant permission to view your device contacts.",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Gray
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
            onConfirm = { name, number, email ->
                viewModel.createContact(name, number, email)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ContactCardItem(
    contact: ContactEntity,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onDelete: () -> Unit
) {
    val initials = if (contact.displayName.isNotBlank()) {
        contact.displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    } else "#"

    val avatarColor = remember(contact.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(contact.colorHex))
        } catch (_: Exception) {
            NeobrutalYellow
        }
    }

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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Call
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
                        .size(36.dp)
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
                        .size(36.dp)
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
    onConfirm: (name: String, number: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

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
                placeholder = "PHONE NUMBER",
                testTag = "input_contact_number"
            )

            NeobrutalTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "EMAIL ADDRESS (OPTIONAL)",
                testTag = "input_contact_email"
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
                            onConfirm(name, number, email)
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
