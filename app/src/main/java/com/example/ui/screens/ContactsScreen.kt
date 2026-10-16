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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.allContacts.collectAsState()
    val hasPermission by viewModel.hasContactsPermission.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedContactId by remember { mutableStateOf<Long?>(null) }
    var contactToEdit by remember { mutableStateOf<ContactEntity?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermissionResult(granted)
    }

    val distinctContacts = remember(contacts) {
        contacts.distinctBy { it.displayName.trim().lowercase() }
    }

    val filteredContacts = remember(distinctContacts, searchQuery) {
        if (searchQuery.isBlank()) distinctContacts else {
            distinctContacts.filter {
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
        // --- Header (padded on right so floating settings icon never overlaps with header or search bar) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(end = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONTACTS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeobrutalBlack
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                items(filteredContacts, key = { "${it.id}_${it.lookupKey}_${it.phoneNumber}_${it.displayName}" }) { contact ->
                    ContactCardItem(
                        contact = contact,
                        isExpanded = expandedContactId == contact.id,
                        onToggleExpand = {
                            expandedContactId = if (expandedContactId == contact.id) null else contact.id
                        },
                        onCall = { viewModel.startCall(contact.phoneNumber, contact.displayName) },
                        onSms = { viewModel.swipeActionSms(contact.phoneNumber) },
                        onEdit = { contactToEdit = contact },
                        onCopyNumber = { viewModel.copyToClipboard(contact.phoneNumber, "Contact Phone Number") },
                        onShare = { viewModel.shareContact(contact.displayName, contact.phoneNumber) },
                        onToggleFavorite = { viewModel.toggleFavorite(contact) },
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

    // --- Edit Contact Dialog ---
    if (contactToEdit != null) {
        val target = contactToEdit!!
        EditContactDialog(
            contact = target,
            onDismiss = { contactToEdit = null },
            onConfirm = { updatedName, updatedNumber, updatedEmail ->
                viewModel.updateContact(target.copy(
                    displayName = updatedName.trim(),
                    phoneNumber = updatedNumber.trim(),
                    normalizedNumber = updatedNumber.filter { it.isDigit() },
                    email = updatedEmail.trim()
                ))
                contactToEdit = null
            }
        )
    }
}

@Composable
fun ContactCardItem(
    contact: ContactEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onEdit: () -> Unit,
    onCopyNumber: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
        testTag = "contact_item_${contact.id}",
        onClick = onToggleExpand,
        onLongClick = { showDeleteDialog = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Row (Clicking toggles expansion in place)
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                // Details (Name & number)
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
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (contact.isStarred) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = NeobrutalYellow,
                                modifier = Modifier.size(16.dp)
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
                }

                // Expand Chevron
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand Contact",
                    tint = NeobrutalBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expanded Info and Actions (Google Phone style in the same place)
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

                    // Contact Details Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9))
                            .border(BorderStroke(1.dp, Color(0xFFDDDDDD)))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Phone Row with quick copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PHONE",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = NeobrutalBlack
                                    )
                                )
                            }
                            NeobrutalIconButton(
                                onClick = onCopyNumber,
                                containerColor = NeobrutalWhite,
                                size = 32.dp,
                                shadowOffset = 1.dp,
                                testTag = "contact_copy_btn_${contact.id}",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Number",
                                        tint = NeobrutalBlack,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        }

                        // Email Row
                        if (contact.email.isNotBlank()) {
                            Column {
                                Text(
                                    text = "EMAIL",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                )
                                Text(
                                    text = contact.email,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = NeobrutalBlack
                                    )
                                )
                            }
                        }

                        // History / Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALL INTERACTIONS: ${contact.callCount} calls",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            )
                            if (contact.lastContactedTimestamp > 0) {
                                val lastContactStr = SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(contact.lastContactedTimestamp))
                                Text(
                                    text = "LAST: $lastContactStr",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }

                        // Star / Favorite toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (contact.isStarred) "STARRED CONTACT" else "NOT STARRED",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = NeobrutalBlack
                                )
                            )
                            NeobrutalButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.height(30.dp),
                                text = if (contact.isStarred) "UNSTAR" else "STAR",
                                containerColor = if (contact.isStarred) NeobrutalYellow else NeobrutalWhite,
                                contentColor = NeobrutalBlack,
                                shadowOffset = 1.dp,
                                testTag = "contact_star_btn_${contact.id}",
                                icon = {
                                    Icon(
                                        imageVector = if (contact.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star",
                                        tint = NeobrutalBlack,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action Buttons: CALL, SMS, EDIT, SHARE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Call
                        NeobrutalButton(
                            onClick = onCall,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            text = "CALL",
                            containerColor = NeobrutalGreen,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 2.dp,
                            testTag = "contact_call_${contact.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // SMS
                        NeobrutalButton(
                            onClick = onSms,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            text = "SMS",
                            containerColor = NeobrutalBlue,
                            contentColor = NeobrutalWhite,
                            shadowOffset = 2.dp,
                            testTag = "contact_sms_${contact.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "SMS",
                                    tint = NeobrutalWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Edit
                        NeobrutalButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            text = "EDIT",
                            containerColor = NeobrutalYellow,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 2.dp,
                            testTag = "contact_edit_${contact.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Share
                        NeobrutalButton(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            text = "SHARE",
                            containerColor = NeobrutalCyan,
                            contentColor = NeobrutalBlack,
                            shadowOffset = 2.dp,
                            testTag = "contact_share_${contact.id}",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        NeobrutalDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "DELETE CONTACT",
            titleColor = NeobrutalRed
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Are you sure you want to delete ${contact.displayName.uppercase()} (${contact.phoneNumber})?",
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
                        testTag = "cancel_delete_contact_btn"
                    )
                    NeobrutalButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        text = "DELETE",
                        containerColor = NeobrutalRed,
                        testTag = "confirm_delete_contact_btn"
                    )
                }
            }
        }
    }
}

@Composable
private fun EditContactDialog(
    contact: ContactEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf(contact.displayName) }
    var number by remember { mutableStateOf(contact.phoneNumber) }
    var email by remember { mutableStateOf(contact.email) }

    NeobrutalDialog(
        onDismissRequest = onDismiss,
        title = "EDIT CONTACT",
        titleColor = NeobrutalYellow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NeobrutalTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "FULL NAME",
                testTag = "input_edit_contact_name"
            )

            NeobrutalTextField(
                value = number,
                onValueChange = { number = it },
                placeholder = "PHONE NUMBER",
                testTag = "input_edit_contact_number"
            )

            NeobrutalTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "EMAIL ADDRESS (OPTIONAL)",
                testTag = "input_edit_contact_email"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeobrutalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    text = "CANCEL",
                    containerColor = NeobrutalWhite,
                    testTag = "cancel_edit_contact_btn"
                )
                NeobrutalButton(
                    onClick = {
                        if (name.isNotBlank() && number.isNotBlank()) {
                            onConfirm(name, number, email)
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    text = "SAVE",
                    containerColor = NeobrutalGreen,
                    testTag = "confirm_edit_contact_btn"
                )
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
