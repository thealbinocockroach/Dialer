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
import com.example.data.model.CallRecordingEntity
import com.example.repository.PlaybackState
import com.example.ui.components.NeobrutalBadge
import com.example.ui.components.NeobrutalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.allRecordings.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalBgLight)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CALL RECORDINGS (${recordings.size})",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeobrutalBlack
                    )
                )
                Text(
                    text = "LOCAL M4A AUDIO CAPTURES",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    )
                )
            }
            NeobrutalBadge(
                text = "AAC AUDIO",
                color = NeobrutalPink,
                textColor = NeobrutalWhite
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recordings.isEmpty()) {
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
                            text = "NO RECORDED CALLS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeobrutalBlack
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enable Auto-Record in Settings or tap RECORD during an active call.",
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    RecordingCardItem(
                        recording = item,
                        playbackState = playbackState,
                        onPlay = { viewModel.playRecording(item) },
                        onPause = { viewModel.pauseRecording() },
                        onDelete = { viewModel.deleteRecording(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCardItem(
    recording: CallRecordingEntity,
    playbackState: PlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit
) {
    val isCurrent = playbackState.recordingId == recording.id
    val isPlaying = isCurrent && playbackState.isPlaying

    val dateStr = remember(recording.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(recording.timestamp))
    }

    val sizeFormatted = remember(recording.fileSizeBytes) {
        val kb = recording.fileSizeBytes / 1024
        if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
    }

    NeobrutalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isPlaying) NeobrutalYellow.copy(alpha = 0.2f) else NeobrutalWhite,
        shadowOffset = 4.dp,
        testTag = "recording_card_${recording.id}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = (if (recording.callerName.isNotBlank()) recording.callerName else recording.callerNumber).uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = NeobrutalBlack
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "${recording.callerNumber} • $dateStr",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isPlaying) NeobrutalPink else NeobrutalGreen)
                            .border(BorderStroke(2.dp, NeobrutalBlack))
                            .clickable { if (isPlaying) onPause() else onPlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NeobrutalBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Delete button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(NeobrutalWhite)
                            .border(BorderStroke(2.dp, NeobrutalBlack))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeobrutalBadge(
                        text = "${recording.durationSeconds} SEC",
                        color = NeobrutalCyan
                    )
                    NeobrutalBadge(
                        text = sizeFormatted,
                        color = NeobrutalWhite
                    )
                }

                if (isPlaying) {
                    Text(
                        text = "PLAYING NOW...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = NeobrutalPink
                        )
                    )
                }
            }
        }
    }
}
