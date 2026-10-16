package com.example.repository

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

class TelephonyService(val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    fun isDefaultDialer(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
                }
            }
            telecomManager?.defaultDialerPackage == context.packageName
        } catch (e: Exception) {
            Log.w("TelephonyService", "Failed to check default dialer status: ${e.message}")
            false
        }
    }

    fun createDefaultDialerIntent(): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                }
            }
            if (telecomManager != null && context.packageName != telecomManager.defaultDialerPackage) {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                }
            } else null
        } catch (e: Exception) {
            Log.w("TelephonyService", "Failed to create default dialer intent: ${e.message}")
            null
        }
    }

    fun canDrawOverlays(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else true
        } catch (e: Throwable) {
            Log.w("TelephonyService", "canDrawOverlays check: ${e.message}")
            true
        }
    }

    fun createOverlaySettingsIntent(): Intent {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        } catch (_: Throwable) {
            Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun placeRealCall(number: String): Boolean {
        return try {
            val clean = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            if (clean.isBlank()) return false
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission && telecomManager != null) {
                val uri = Uri.fromParts("tel", clean, null)
                val extras = android.os.Bundle()
                telecomManager.placeCall(uri, extras)
                Log.d("TelephonyService", "Placed real call via TelecomManager to $clean")
                true
            } else {
                launchSystemDialer(clean, useDirectCallIfPermitted = true)
                true
            }
        } catch (e: Exception) {
            Log.e("TelephonyService", "Failed to place real call via TelecomManager, fallback to intent", e)
            try {
                launchSystemDialer(number, useDirectCallIfPermitted = true)
                true
            } catch (e2: Exception) {
                Log.e("TelephonyService", "Failed to launch fallback dialer intent", e2)
                false
            }
        }
    }

    fun executeUssdOrCall(number: String): Boolean {
        return try {
            val encoded = Uri.encode(number)
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$encoded"))
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded"))
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("TelephonyService", "Failed to execute call/USSD: ${e.message}")
            false
        }
    }

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
        } catch (e: Throwable) {
            Log.e("TelephonyService", "Failed to launch phone intent", e)
            // Fallback to dial
            try {
                val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (e2: Throwable) {
                Log.e("TelephonyService", "Failed to launch fallback dialer", e2)
            }
        }
    }

    fun launchSmsApp(phoneNumber: String, draftMessage: String = "") {
        try {
            val clean = Uri.encode(phoneNumber)
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (draftMessage.isNotEmpty()) {
                    putExtra("sms_body", draftMessage)
                }
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e("TelephonyService", "Failed to launch sms app: ${e.message}")
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

    fun copyToClipboard(text: String, label: String = "Text"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            Log.w("TelephonyService", "Failed to copy to clipboard: ${e.message}")
            false
        }
    }

    fun shareContact(name: String, number: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Contact: $name")
                putExtra(Intent.EXTRA_TEXT, "$name: $number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(shareIntent, "Share Contact").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.w("TelephonyService", "Share contact error: ${e.message}")
        }
    }
}
