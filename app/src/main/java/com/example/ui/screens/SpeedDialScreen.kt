package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.NeobrutalBadge
import com.example.ui.components.NeobrutalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun SpeedDialScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val speedDialContacts by viewModel.speedDialContacts.collectAsState()
    val speedDialMap = speedDialContacts.associateBy { it.speedDialDigit ?: 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SPEED DIAL MATRIX",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeobrutalBlack
                    )
                )
                Text(
                    text = "LONG PRESS DIGIT ON KEYPAD OR TAP BELOW TO DIAL",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.DarkGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid for keys 2 to 9
        val slots = (2..9).toList()

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(slots) { digit ->
                val contact = speedDialMap[digit]
                SpeedDialCardSlot(
                    digit = digit,
                    contact = contact,
                    onCall = {
                        if (contact != null) {
                            viewModel.startCall(contact.phoneNumber, contact.displayName)
                        } else {
                            viewModel.selectTab(com.example.ui.viewmodel.DialerTab.CONTACTS)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SpeedDialCardSlot(
    digit: Int,
    contact: ContactEntity?,
    onCall: () -> Unit
) {
    val bgColor = if (contact != null) {
        Color(android.graphics.Color.parseColor(contact.colorHex))
    } else NeobrutalWhite

    NeobrutalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = bgColor,
        shadowOffset = 4.dp,
        testTag = "speed_dial_slot_$digit",
        onClick = onCall
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Slot number badge & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeobrutalBlack)
                        .border(BorderStroke(2.dp, NeobrutalBlack)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$digit",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeobrutalYellow
                        )
                    )
                }

                if (contact != null) {
                    NeobrutalBadge(
                        text = "CALL",
                        color = NeobrutalGreen
                    )
                } else {
                    NeobrutalBadge(
                        text = "EMPTY",
                        color = NeobrutalGrayLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            if (contact != null) {
                Column {
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
                            fontSize = 10.sp,
                            color = Color(0xFF333333)
                        ),
                        maxLines = 1
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Assign",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TAP TO ASSIGN",
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
    }
}
