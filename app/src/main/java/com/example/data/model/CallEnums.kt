package com.example.data.model

enum class CallState {
    IDLE,
    RINGING,
    OFFHOOK,
    DISCONNECTED
}

enum class CallStage {
    DIALING,         // "Dialing..."
    CONNECTING,      // "Connecting..."
    RINGING,         // "Ringing..."
    CONNECTED,       // "Connected"
    ON_HOLD,         // "Call on Hold"
    BUSY,            // "Line Busy"
    ENDED            // "Call Ended"
}

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    BLOCKED
}

enum class OverlayMode {
    FULL_SCREEN,
    POP_UP,
    MINI_POP_UP
}

enum class AutoRecordRule {
    ALL,
    UNSAVED,
    SPECIFIC_CONTACTS,
    OFF
}
