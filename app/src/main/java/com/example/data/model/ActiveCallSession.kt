package com.example.data.model

data class ActiveCallSession(
    val callId: String = "",
    val number: String,
    val callerName: String = "",
    val contactInitials: String = "?",
    val colorHex: String = "#FFE600",
    val callState: CallState = CallState.IDLE,
    val callStage: CallStage = CallStage.DIALING,
    val statusText: String = "DIALING...",
    val isIncoming: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isHold: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val callDurationSeconds: Int = 0,
    val recordingFilePath: String? = null,
    val overlayMode: OverlayMode = OverlayMode.FULL_SCREEN,
    val isBlocked: Boolean = false,
    val startTimeMillis: Long = System.currentTimeMillis()
)
