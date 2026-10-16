package com.example.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android Telecom InCallService implementation.
 * Required for the application to be qualified and operational as the Android Default Phone App.
 * When the app is chosen as the system default dialer, Android binds to this service for incoming
 * and outgoing telecom phone calls.
 */
class BrutalInCallService : InCallService() {

    companion object {
        const val INCOMING_CALL_CHANNEL_ID = "incoming_call_channel"
        const val INCOMING_CALL_NOTIFICATION_ID = 2001

        var currentCall: Call? = null
            private set

        var secondaryCall: Call? = null
            private set

        private var serviceInstance: BrutalInCallService? = null

        private val _callFlow = MutableStateFlow<Call?>(null)
        val callFlow: StateFlow<Call?> = _callFlow.asStateFlow()

        private val _secondaryCallFlow = MutableStateFlow<Call?>(null)
        val secondaryCallFlow: StateFlow<Call?> = _secondaryCallFlow.asStateFlow()

        private val listeners = java.util.concurrent.CopyOnWriteArrayList<InCallListener>()

        fun addListener(listener: InCallListener) {
            try {
                if (!listeners.contains(listener)) {
                    listeners.add(listener)
                }
            } catch (e: Throwable) {
                Log.w("BrutalInCallService", "Failed to add listener", e)
            }
        }

        fun removeListener(listener: InCallListener) {
            try {
                listeners.remove(listener)
            } catch (e: Throwable) {
                Log.w("BrutalInCallService", "Failed to remove listener", e)
            }
        }

        fun getCallNumber(call: Call?): String {
            return try {
                val handle = call?.details?.handle ?: return ""
                handle.schemeSpecificPart ?: handle.toString().removePrefix("tel:")
            } catch (e: Throwable) {
                ""
            }
        }

        fun answerCurrentCall() {
            try {
                Log.d("BrutalInCallService", "Answering current call")
                val call = currentCall
                if (call != null && call.state == Call.STATE_RINGING) {
                    call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to answer telecom call", e)
            }
        }

        fun disconnectCurrentCall() {
            try {
                val call = currentCall
                Log.d("BrutalInCallService", "Disconnecting call")
                if (call != null) {
                    if (call.state == Call.STATE_RINGING) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            call.reject(Call.REJECT_REASON_DECLINED)
                        } else {
                            call.disconnect()
                        }
                    } else {
                        call.disconnect()
                    }
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to disconnect telecom call", e)
            }
        }

        fun answerSecondaryCall(holdPrimary: Boolean = true) {
            try {
                if (holdPrimary) {
                    currentCall?.hold()
                } else {
                    currentCall?.disconnect()
                }
                secondaryCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to answer secondary call", e)
            }
        }

        fun mergeCalls() {
            try {
                val primary = currentCall
                val second = secondaryCall
                if (primary != null && second != null) {
                    primary.conference(second)
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to merge calls into conference", e)
            }
        }

        fun swapCalls() {
            try {
                val primary = currentCall
                val second = secondaryCall
                if (primary != null && second != null) {
                    if (primary.state == Call.STATE_HOLDING) {
                        second.hold()
                        primary.unhold()
                    } else if (second.state == Call.STATE_HOLDING) {
                        primary.hold()
                        second.unhold()
                    }
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to swap calls", e)
            }
        }

        fun declineSecondaryCall() {
            try {
                val second = secondaryCall
                if (second != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        second.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        second.disconnect()
                    }
                }
                secondaryCall = null
                _secondaryCallFlow.value = null
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to decline secondary call", e)
            }
        }

        fun holdCurrentCall(hold: Boolean) {
            try {
                val call = currentCall ?: return
                if (hold) {
                    call.hold()
                } else {
                    call.unhold()
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to toggle hold telecom call", e)
            }
        }

        fun setMuted(mute: Boolean) {
            try {
                serviceInstance?.setMuted(mute)
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to set mic muted", e)
            }
        }

        fun setSpeakerphone(speaker: Boolean) {
            try {
                val route = if (speaker) {
                    android.telecom.CallAudioState.ROUTE_SPEAKER
                } else {
                    android.telecom.CallAudioState.ROUTE_EARPIECE
                }
                serviceInstance?.setAudioRoute(route)
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to set audio route", e)
            }
        }
    }

    interface InCallListener {
        fun onCallAdded(call: Call)
        fun onCallRemoved(call: Call)
        fun onCallStateChanged(call: Call, state: Int)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            try {
                Log.d("BrutalInCallService", "Telecom Call state changed: $state")
                if (call == currentCall) {
                    _callFlow.value = call
                } else if (call == secondaryCall) {
                    _secondaryCallFlow.value = call
                }

                if (state == Call.STATE_DISCONNECTED) {
                    cancelIncomingCallNotification()
                    if (call == currentCall) {
                        if (secondaryCall != null) {
                            currentCall = secondaryCall
                            secondaryCall = null
                            _callFlow.value = currentCall
                            _secondaryCallFlow.value = null
                        } else {
                            currentCall = null
                            _callFlow.value = null
                        }
                    } else if (call == secondaryCall) {
                        secondaryCall = null
                        _secondaryCallFlow.value = null
                    }
                } else if (state != Call.STATE_RINGING) {
                    cancelIncomingCallNotification()
                }

                for (listener in listeners) {
                    try {
                        listener.onCallStateChanged(call, state)
                    } catch (t: Throwable) {
                        Log.e("BrutalInCallService", "Listener error in onCallStateChanged", t)
                    }
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Error in callCallback onStateChanged", e)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        try {
            Log.d("BrutalInCallService", "Telecom Call added: ${call.details?.handle}")
            if (currentCall == null || currentCall?.state == Call.STATE_DISCONNECTED) {
                currentCall = call
                _callFlow.value = call
            } else {
                secondaryCall = call
                _secondaryCallFlow.value = call
            }
            try {
                call.registerCallback(callCallback)
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to register callback on call", e)
            }

            for (listener in listeners) {
                try {
                    listener.onCallAdded(call)
                } catch (t: Throwable) {
                    Log.e("BrutalInCallService", "Listener error in onCallAdded", t)
                }
            }

            if (call.state == Call.STATE_RINGING) {
                try {
                    showIncomingCallNotificationAndActivity(call)
                } catch (e: Throwable) {
                    Log.e("BrutalInCallService", "Error in showIncomingCallNotificationAndActivity", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("BrutalInCallService", "Fatal error in onCallAdded", e)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        try {
            Log.d("BrutalInCallService", "Telecom Call removed")
            cancelIncomingCallNotification()
            try {
                call.unregisterCallback(callCallback)
            } catch (e: Throwable) {
                Log.w("BrutalInCallService", "Failed to unregister callback: ${e.message}")
            }
            if (call == currentCall) {
                if (secondaryCall != null) {
                    currentCall = secondaryCall
                    secondaryCall = null
                    _callFlow.value = currentCall
                    _secondaryCallFlow.value = null
                } else {
                    currentCall = null
                    _callFlow.value = null
                }
            } else if (call == secondaryCall) {
                secondaryCall = null
                _secondaryCallFlow.value = null
            }

            for (listener in listeners) {
                try {
                    listener.onCallRemoved(call)
                } catch (t: Throwable) {
                    Log.e("BrutalInCallService", "Listener error in onCallRemoved", t)
                }
            }
        } catch (e: Throwable) {
            Log.e("BrutalInCallService", "Fatal error in onCallRemoved", e)
        }
    }

    private fun showIncomingCallNotificationAndActivity(call: Call) {
        val number = getCallNumber(call)
        Log.d("BrutalInCallService", "Showing incoming call activity for: $number")
        
        try {
            createNotificationChannel()

            val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_INCOMING_CALL", true)
                putExtra("EXTRA_INCOMING_NUMBER", number)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                1001,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Notification Action: Decline
            val declineIntent = Intent(this, CallNotificationReceiver::class.java).apply {
                action = CallNotificationReceiver.ACTION_DECLINE
            }
            val declinePendingIntent = PendingIntent.getBroadcast(
                this,
                1002,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Notification Action: Answer
            val answerIntent = Intent(this, CallNotificationReceiver::class.java).apply {
                action = CallNotificationReceiver.ACTION_ANSWER
            }
            val answerPendingIntent = PendingIntent.getBroadcast(
                this,
                1003,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callerDisplay = if (number.isNotBlank()) number else "Unknown Caller"

            // Use android.R.drawable.stat_sys_phone_call for safe smallIcon across all Android versions
            val notification = NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_phone_call)
                .setContentTitle("INCOMING CALL")
                .setContentText(callerDisplay)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullScreenPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DECLINE", declinePendingIntent)
                .addAction(android.R.drawable.ic_menu_call, "ANSWER", answerPendingIntent)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
            
            // Bring app directly to front safely without crashing if background launch is restricted
            try {
                startActivity(fullScreenIntent)
            } catch (e: Throwable) {
                Log.w("BrutalInCallService", "Direct startActivity skipped/restricted: ${e.message}")
            }
            
        } catch (e: Throwable) {
            Log.e("BrutalInCallService", "Failed to show incoming call notification: ${e.message}", e)
        }
    }

    private fun cancelIncomingCallNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w("BrutalInCallService", "Could not cancel notification: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val existing = notificationManager?.getNotificationChannel(INCOMING_CALL_CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        INCOMING_CALL_CHANNEL_ID,
                        "Incoming Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "High-priority incoming call alerts on top of other apps"
                        try {
                            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            if (ringtoneUri != null) {
                                val audioAttributes = AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .build()
                                setSound(ringtoneUri, audioAttributes)
                            }
                        } catch (re: Throwable) {
                            Log.w("BrutalInCallService", "Default ringtone setup skipped: ${re.message}")
                        }
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    notificationManager?.createNotificationChannel(channel)
                }
            } catch (e: Throwable) {
                Log.e("BrutalInCallService", "Failed to create incoming call notification channel", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceInstance = null
        currentCall = null
        _callFlow.value = null
    }
}
