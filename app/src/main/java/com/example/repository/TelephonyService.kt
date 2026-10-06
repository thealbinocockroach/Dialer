package com.example.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

class TelephonyService(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun launchSystemDialer(phoneNumber: String, useDirectCallIfPermitted: Boolean = true) {
        val clean = Uri.encode(phoneNumber)
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (useDirectCallIfPermitted && hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TelephonyService", "Failed to launch phone intent", e)
            // Fallback to dial
            val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    fun launchSmsApp(phoneNumber: String, draftMessage: String = "") {
        val clean = Uri.encode(phoneNumber)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (draftMessage.isNotEmpty()) {
                putExtra("sms_body", draftMessage)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TelephonyService", "Failed to open SMS app", e)
        }
    }

    fun sendQuickDeclineSms(phoneNumber: String, message: String): Boolean {
        return try {
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSmsPermission) {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                true
            } else {
                // Launch SMS intent as graceful fallback
                launchSmsApp(phoneNumber, message)
                true
            }
        } catch (e: Exception) {
            Log.w("TelephonyService", "Failed to send SMS directly: ${e.message}")
            launchSmsApp(phoneNumber, message)
            false
        }
    }

    fun setSpeakerphone(on: Boolean) {
        try {
            audioManager?.isSpeakerphoneOn = on
        } catch (e: Exception) {
            Log.w("TelephonyService", "Speaker toggle error: ${e.message}")
        }
    }

    fun setMicrophoneMute(mute: Boolean) {
        try {
            audioManager?.isMicrophoneMute = mute
        } catch (e: Exception) {
            Log.w("TelephonyService", "Mic mute toggle error: ${e.message}")
        }
    }

    fun isHeadsetOrBluetoothConnected(): Boolean {
        if (audioManager == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> return true
                }
            }
        } else {
            @Suppress("DEPRECATION")
            return audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
        }
        return false
    }
}
