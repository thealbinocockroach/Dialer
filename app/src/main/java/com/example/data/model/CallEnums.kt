package com.example.data.model

enum class CallState {
    IDLE,
    RINGING,
    OFFHOOK,
    DISCONNECTED
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
